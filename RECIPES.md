# Рецепты HBM Mayonnaise

Ресурсы игровых рецептов: `src/main/resources/data/hbmmayo/recipe/`.
Форматы машинных рецептов и ID компонентов сверены с предоставленным JAR.
Загрузка этих рецептов в игре пока не проверена.

| Машина | Вход | Выход | Базовое время / энергия |
|---|---|---|---|
| Industrial Mixer | 700 mB `hbm:sunfloweroil`, 300 mB `hbm:egg`, 1 `hbm:niter` | 1000 mB Mayonnaise Emulsion | 160 тиков, штатный расход Mixer |
| Chemical Plant | 1000 mB эмульсии, 100 mB `hbm:salient`, 1 `hbm:sulfur` | 1000 mB Industrial Mayonnaise | 200 тиков, 125 HE/t |
| Chemical Plant | 1000 mB Industrial Mayonnaise, 4 пустые банки | 4 банки по 250 mB | 80 тиков, 50 HE/t |
| PUREX | 1 `hbm:billet_balefire_gold`, 1 `hbm:pellet_charged`, 500 mB `hbm:salient` | 500 mB Flashgold Colloid | 400 тиков, 1000 HE/t |
| Industrial Mixer | 1000 mB эмульсии, 500 mB коллоида | 1500 mB Flashgold Mayonnaise | 160 тиков, штатный расход Mixer |
| Chemical Plant | 250 mB Flashgold Mayonnaise, 1 экранированная банка | 1 банка | 80 тиков, 50 HE/t |
| Chemical Plant | 100 mB коллоида, 1 пустая капсула | 1 капсула коллоида | 80 тиков, 50 HE/t |
| Chemical Plant | 100 mB коллоида, 1 пустой инъектор, 1 Circuit | 1 стабилизирующий инъектор | 200 тиков, 125 HE/t |

Одна партия Flashgold Mayonnaise даёт шесть банок. Salient Green в обычном
майонезе расходуется реагентом и не увеличивает объём. Ionized Particles —
нативный расходуемый `hbm:pellet_charged`, по одному на операцию PUREX, как в
производстве Flashgold основного мода. Нативные цепочки масла, яйца и Salient Green
сохранены без дублирования. Фасовка потребляет контейнер ровно один раз.

Энергия Mixer намеренно оставлена нативной по указанию пользователя; отдельной
правки на 125 HE/t нет. Значения других неуказанных в ТЗ процессов выбраны аддоном.

## Компоненты

В этой версии часть старых схем представлена circuit blocks. Применяются:

| Роль | Реальный ID |
|---|---|
| Circuit | `hbm:cm_circuit_aluminium`, Tier 1 Circuit Block |
| Advanced Circuit | `hbm:cm_circuit_red_copper`, Tier 3 Circuit Block |
| Gold Wire | `hbm:wire_fine_gold` |
| Electricity Connector | `hbm:red_connector` |
| Energy Cell alternative для камеры | `hbm:battery_pack_capacitor_copper` |
| Steel / Titanium / Lead Plate | `hbm:plate_steel` / `hbm:plate_titanium` / `hbm:plate_lead` |

Connector присутствует, поэтому comparator fallback для этой версии не нужен.
Оба броневых модуля обходятся без батарей. Дополнительный Machine Frame —
`hbmmayo:machine_frame`, из 4 Steel Plate и 1 Iron Block; новой батареи нет.

## Верстак 3×3

- Passive Stabilizer: `SCS / GFG / SES`; S Steel Plate, C Circuit, G Gold Wire,
  F капсула коллоида, E Electricity Connector.
- Entropy Reclaimer: `TAT / GFG / TET`; T Titanium Plate, A Advanced Circuit;
  остальные буквы как выше.
- Камера: `SAS / EFE / SMS`; S Steel Plate, A Advanced Circuit, E медный
  HE-capacitor, F капсула коллоида, M Machine Frame.
- Адаптер: `SCS / PMP / SFS`; S Steel Plate, C Circuit, P Lead Plate,
  M Machine Frame, F пустая банка.
- Machine Frame: ` S  / SIS /  S ` в виде креста (точный рисунок в JSON).

## Вспомогательные предметы

- Пустая банка: 3 Glass Pane + 1 Iron Nugget → 1.
- Экранированная банка: 1 пустая банка + 2 Lead Plate + 1 Steel Plate → 1.
- Пустая капсула: 2 Glass Pane + 1 Steel Plate → 1.
- Пустой инъектор: 1 Glass Pane + 1 Iron Nugget + 1 Steel Plate → 1.

Успешное употребление Flashgold Mayonnaise возвращает экранированную банку.
Повторное употребление при активном Infusion отклоняется без расходования.
