package dev.hbmmayo.core;
import java.io.*;
import java.util.*;

public final class CoreChecks {
    static int checks;
    static final MayoConfig C = MayoConfig.defaults();
    static final EnergyStore POWER = n -> true;
    static final EnergyStore EMPTY = n -> false;
    static void check(boolean condition, String message) {
        checks++; if (!condition) throw new AssertionError(message);
    }
    static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 1e-8, message + ": " + actual + " != " + expected);
    }
    static InfusionState player(double points) {
        var s = new InfusionState(); s.activate();
        s.healthLost(2 * (points / 5 + .5), 20, C);
        return s;
    }
    static InfusionState roundTrip(InfusionState s) throws Exception {
        var bytes = new ByteArrayOutputStream(); s.write(new DataOutputStream(bytes));
        return InfusionState.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
    }
    static CoolantTank tankRoundTrip(CoolantTank s) throws Exception {
        var bytes = new ByteArrayOutputStream(); s.write(new DataOutputStream(bytes));
        return CoolantTank.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
    }
    static final class Port implements PlayerController.PlayerPort {
        InfusionBonuses bonuses;
        double healed;
        int syncs, deaths;
        public void reconcileBonuses(InfusionBonuses b) { bonuses=b; }
        public void heal(double hp) { healed+=hp; }
        public InfusionState.ChestModule chestModule() { return InfusionState.ChestModule.NONE; }
        public EnergyStore armorEnergy() { return POWER; }
        public void synchronize(InfusionState.Snapshot s) { syncs++; }
        public void annihilate() { deaths++; }
    }
    public static void main(String[] args) throws Exception {
        double[][] examples = {{20,1,0},{20,2,2.5},{20,8,17.5},{40,8,35.0/3},{80,8,7}};
        for (var e : examples) {
            var s = player(0); s.healthLost(e[1],e[0],C); near(s.instability(),e[2],"formula");
        }
        var s = new InfusionState();
        check(s.activate(),"first jar accepted"); check(!s.activate(),"second jar rejected");
        near(s.healthLost(0,20,C).addedPoints(),0,"DT/shield fully absorbs");
        near(s.healthLost(1,20,C).addedPoints(),0,"half-heart grace");
        for (int i=0;i<160;i++) s.tick(InfusionState.ChestModule.NONE,POWER,C);
        int quiet = s.quietTicks(); s.healthLost(0,20,C);
        check(s.quietTicks()==quiet,"shield does not reset quiet timer");
        s.healthLost(.1,20,C); check(s.quietTicks()==0,"small actual damage resets timer even with zero delta");
        var ledger = new DamageLedger(); ledger.begin(20); ledger.begin(18);
        near(ledger.end(16),2,"nested own loss"); near(ledger.end(14),4,"outer excludes nested");
        ledger.begin(14); near(ledger.end(14),0,"absorbed invocation");
        ledger.begin(20);ledger.healthChanged(20,12);ledger.healthChanged(12,14);
        near(ledger.end(14),8,"healing in the same damage invocation must not hide actual HP lost");
        ledger.begin(20);ledger.healthChanged(20,18);ledger.begin(18);
        ledger.healthChanged(18,14);ledger.healthChanged(14,15);
        near(ledger.end(15),4,"nested damage excludes its own healing");
        ledger.healthChanged(15,14);
        near(ledger.end(14),3,"outer tracked health writes exclude nested damage");
        s = player(99); near(s.instability(),99,"99 remains intact");
        var b = InfusionBonuses.forState(s,C);
        near(b.damageThresholdHp(),3,"native DT target"); near(b.attackMultiplier(),1.3,"attack");
        near(b.movementMultiplier(),1.2,"speed");
        check(s.healthLost(2,20,C).annihilate(),"100 requests immediate annihilation");
        s.die(); check(!s.active(),"death removes infusion"); near(s.instability(),0,"death clears instability");
        near(InfusionBonuses.forState(s,C).attackMultiplier(),1,"death removes bonus");
        s = player(42); check(s.kill(InfusionState.ChestModule.ENTROPY,POWER,C),"entropy kill");
        near(s.instability(),35,"subtract exactly seven points");
        check(!s.kill(InfusionState.ChestModule.ENTROPY,EMPTY,C),"kill requires energy");
        near(s.instability(),35,"no free stabilization");
        var kills = new KillLedger(); var victim = new KillLedger.VictimLife();
        check(kills.death(victim,s,InfusionState.ChestModule.ENTROPY,POWER,C),"one death");
        check(!kills.death(victim,s,InfusionState.ChestModule.ENTROPY,POWER,C),"duplicate death ignored");
        near(s.instability(),28,"duplicate not paid twice");
        for(int i=0;i<20;i++) kills.death(new KillLedger.VictimLife(),s,InfusionState.ChestModule.ENTROPY,POWER,C);
        near(s.instability(),0,"mass kills clamp at zero");
        s = player(42); for(int i=0;i<200;i++) s.tick(InfusionState.ChestModule.PASSIVE,EMPTY,C);
        near(s.instability(),42,"unpowered passive");
        for(int i=0;i<20;i++) s.tick(InfusionState.ChestModule.PASSIVE,POWER,C);
        near(s.instability(),41.5,"passive .5 points per second");
        double before=s.instability(); for(int i=0;i<100;i++) s.tick(InfusionState.ChestModule.NONE,POWER,C);
        near(s.instability(),before,"removed armor does not stabilize");
        s = player(42); check(s.inject(C),"injector succeeds"); near(s.instability(),17,"injector 25 points");
        check(!s.inject(C),"injector cooldown"); s=roundTrip(s);
        near(s.instability(),17,"persist instability"); check(s.injectorCooldown()==1200,"persist cooldown");
        for(int i=0;i<1200;i++) s.tick(InfusionState.ChestModule.NONE,POWER,C);
        check(s.inject(C),"injector after cooldown"); near(s.instability(),0,"injector lower clamp");
        check(!new InfusionState().inject(C),"no infusion no injector consumption");
        s=player(10); var chamber=new Chamber();
        var outOfRange=player(10); var targets=List.of(new Chamber.Target(s,9),new Chamber.Target(s,9),new Chamber.Target(outOfRange,9.01));
        for(int i=0;i<20;i++) chamber.tick(targets,EMPTY,C);
        near(s.instability(),10,"chamber requires HE");
        int affected=0; for(int i=0;i<20;i++) affected+=chamber.tick(targets,POWER,C);
        check(affected==1,"chamber deduplicates target"); near(s.instability(),9,"chamber one point");
        near(outOfRange.instability(),10,"chamber radius");
        var t=new CoolantTank(1000); t.fillIndustrial(1);
        for(int i=0;i<1000;i++) t.afterReactorTick(false,C);
        check(t.usedTicks()==0,"idle coolant never ages");
        for(int i=0;i<300;i++) check(!t.afterReactorTick(true,C),"no early burnout");
        t.fillIndustrial(999); check(t.usedTicks()==300,"topup does not reset"); t=tankRoundTrip(t);
        check(t.usedTicks()==300,"tank age persists");
        for(int i=0;i<299;i++) t.afterReactorTick(true,C);
        check(t.canCool(C),"cool until last service tick");
        check(t.afterReactorTick(true,C),"burns at exactly tick 600");
        check(t.burnt() && !t.canCool(C),"burnt cannot cool"); check(t.fillIndustrial(1)==0,"burnt blocks topup");
        t.drain(999); check(t.burnt(),"partial drain preserves burnt state");
        t.drain(1); check(!t.burnt() && t.usedTicks()==0,"full purge resets service");
        t.fillIndustrial(10); t.afterReactorTick(true,C); t.drain(10); t.fillIndustrial(10);
        check(t.usedTicks()==1,"fresh drain/refill cannot reset clock");
        near(Lubrication.efficiency(true,20,C),.85,"mayo lubricant factor");
        near(Lubrication.efficiency(true,C.lubricantMaxTemperature(),C),0,"overheat disables lubrication");
        near(Lubrication.efficiency(false,20,C),0,"other fluids not substituted");
        var hud=new ClientSnapshot(); check(!hud.visible(),"HUD hidden without state");
        hud.accept(player(75).snapshot()); check(hud.visible() && hud.warning(),"HUD threshold");
        check(hud.label(false).equals("FLASHGOLD INSTABILITY: 75.0%"),"HUD precise text");
        hud.resetConnection(); check(!hud.visible(),"connection reset");
        for(var r:RecipeCatalog.ALL) {
            Map<String,Long> caps=new HashMap<>(); r.inputs().forEach((k,v)->caps.put(k,100000L)); r.outputs().forEach((k,v)->caps.put(k,100000L));
            var stock=new Production.Stock(caps); r.inputs().forEach(stock::set);
            var process=new Production.Process(); check(!process.tick(r,stock,EMPTY),"machine requires power");
            for(int i=0;i<r.ticks()-1;i++) check(!process.tick(r,stock,POWER),"wait processing time");
            check(process.tick(r,stock,POWER),"recipe finishes");
            for(var e:r.outputs().entrySet()) near(stock.get(e.getKey()),e.getValue(),"exact recipe output");
            for(var e:r.inputs().entrySet()) near(stock.get(e.getKey()),0,"exact recipe consumption");
            check(!process.tick(r,stock,POWER),"cannot duplicate output");
        }
        var r=RecipeCatalog.get("hbmmayo:industrial_filling");
        var caps=new HashMap<String,Long>(); r.inputs().forEach((k,v)->caps.put(k,v)); r.outputs().forEach((k,v)->caps.put(k,0L));
        var stock=new Production.Stock(caps); r.inputs().forEach(stock::set);
        check(!stock.exchange(r.inputs(),r.outputs()),"full output prevents transaction");
        near(stock.get("item:hbmmayo:empty_mayonnaise_jar"),4,"blocked output does not eat jars");
        var carried=new CoolantTank(16000);carried.fillIndustrial(250);
        for(int i=0;i<599;i++) carried.afterReactorTick(true,C);
        var restored=MachineStateCodec.decode(MachineStateCodec.encode(12500,carried));
        check(restored.power()==12500,"machine energy survives move");
        check(restored.coolant().usedTicks()==599,"block move does not reset service lifetime");
        check(restored.coolant().afterReactorTick(true,C),"carried coolant burns on next working tick");
        var carriedBurnt=MachineStateCodec.decode(MachineStateCodec.encode(restored.power(),restored.coolant()));
        check(carriedBurnt.coolant().burnt(),"burnt state survives block move");
        check(carriedBurnt.coolant().fillIndustrial(100)==0,"carried burnt coolant rejects fresh topup");
        var configProperties=new Properties();
        configProperties.setProperty("killPoints","7");
        near(ConfigFile.parse(configProperties).server().killPoints(),7,"config parser");
        configProperties.setProperty("killPoints","NaN");
        boolean rejected=false;
        try { ConfigFile.parse(configProperties); } catch(IOException expected) { rejected=true; }
        check(rejected,"config rejects nonfinite settings");
        configProperties.clear(); configProperties.setProperty("typo","1"); rejected=false;
        try { ConfigFile.parse(configProperties); } catch(IOException expected) { rejected=true; }
        check(rejected,"config rejects unknown settings");
        var controller = new PlayerController(new InfusionState()); var port = new Port();
        check(controller.consumeFlashgoldJar(port,C),"controller activation");
        check(!controller.consumeFlashgoldJar(port,C),"controller rejects repeat jar");
        int sent=port.syncs;
        controller.connectedOrChangedDimension(port,C);
        check(port.syncs==sent+1,"dimension forces owner sync");
        near(port.bonuses.attackMultiplier(),1.3,"reconcile without stacking");
        for(int i=0;i<100;i++) controller.tick(port,C);
        near(port.healed,1,"regen one HP in five seconds");
        controller.beforeDamage(20); controller.afterDamage(20,20,port,C);
        near(controller.state().instability(),0,"shield HP excluded through orchestration");
        // Use large health to make a single legal ordinary-health loss reach 100.
        controller.beforeDamage(200); controller.afterDamage(0,200,port,C);
        // This adds 90.45 points; a subsequent 30 HP hit crosses the threshold.
        controller.beforeDamage(200); controller.afterDamage(170,200,port,C);
        check(port.deaths==1,"annihilation called exactly once");
        check(!controller.state().active(),"annihilation clears state before native death");
        near(port.bonuses.damageThresholdHp(),0,"annihilation removes DT");
        controller.death(port,C); near(controller.state().instability(),0,"death callback idempotent values");
        var independent=player(42); near(independent.instability(),42,"per-player isolation");
        s=player(10);
        for(int i=0;i<179;i++) s.tick(InfusionState.ChestModule.PASSIVE,POWER,C);
        near(s.instability(),10,"passive waits eight seconds and one full service second");
        s.tick(InfusionState.ChestModule.PASSIVE,POWER,C); near(s.instability(),9.5,"first passive reduction at tick180");
        System.out.println("PASS: " + checks + " core assertions (not Minecraft integration tests)");
    }
}
