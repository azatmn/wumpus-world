// Сборка презентации по лабораторной работе № 3 (pptxgenjs)
const path = require('path');
const fs = require('fs');
const pptxgen = require('pptxgenjs');

const PROJECT = '/Users/azatminyazov/Workspace/university java/3 work';
const OUT = process.env.OUT || path.join(PROJECT, 'docs/presentation/Презентация_ЛР3_Минязов.pptx');
const shot = n => path.join(PROJECT, 'docs/screenshots', n);
const uml = n => path.join(PROJECT, 'docs/uml', n);

// Палитра окна программы: графит, охотник, Вампус, золото, «OK»
const C = {
  dark: '2C2C2A', ink: '2C2C2A', muted: '6B6A65', light: 'F7F6F3', line: 'E4E2DC',
  path: '1D9E75', pathBg: 'E1F5EE', water: '185FA5', waterBg: 'E6F1FB', okText: '085041',
  shock: 'A32D2D', shockBg: 'FCEBEB', cheese: 'BA7517', cheeseBg: 'FAEEDA', white: 'FFFFFF',
};
const HEAD = 'Cambria';
const BODY = 'Calibri';

function pngSize(file) {
  const b = fs.readFileSync(file);
  return { w: b.readUInt32BE(16), h: b.readUInt32BE(20) };
}

/** Картинка, вписанная в прямоугольник с сохранением пропорций (по центру). */
function fit(slide, file, x, y, w, h, extra = {}) {
  const s = pngSize(file);
  const k = Math.min(w / s.w, h / s.h);
  const iw = s.w * k, ih = s.h * k;
  slide.addImage({ path: file, x: x + (w - iw) / 2, y: y + (h - ih) / 2, w: iw, h: ih, ...extra });
  return { x: x + (w - iw) / 2, y: y + (h - ih) / 2, w: iw, h: ih };
}

function title(slide, text, sub) {
  slide.addText(text, { x: 0.6, y: 0.4, w: 12.1, h: 0.8, fontFace: HEAD, fontSize: 36, bold: true, color: C.ink, margin: 0, isTextBox: true });
  if (sub) {
    slide.addText(sub, { x: 0.6, y: 1.15, w: 12.1, h: 0.45, fontFace: BODY, fontSize: 16, color: C.muted, margin: 0, isTextBox: true });
  }
}

function card(slide, x, y, w, h, fill) {
  slide.addShape('roundRect', { x, y, w, h, fill: { color: fill }, line: { color: fill }, rectRadius: 0.12 });
}

/** Кружок с цифрой или знаком — общий мотив всех слайдов. */
function badge(slide, x, y, d, fill, text, color = C.white, size = 16) {
  slide.addShape('ellipse', { x, y, w: d, h: d, fill: { color: fill }, line: { color: fill } });
  slide.addText(text, { x, y, w: d, h: d, align: 'center', valign: 'middle', fontFace: BODY, fontSize: size, bold: true, color, margin: 0, isTextBox: true });
}

// цвета ролей, как в окне
const HUNTER = C.water, WUMPUS = C.shock, GOLD = C.cheese, OK = C.path;

const pres = new pptxgen();
pres.layout = 'LAYOUT_WIDE';  // 13.33 x 7.5
pres.author = 'А. А. Минязов';
pres.title = 'Мир Вампуса — агент, основанный на знаниях';

const bullets = (slide, items, box, size = 17) => slide.addText(
  items.map((t, i) => ({ text: t, options: { bullet: true, breakLine: i < items.length - 1 } })),
  { ...box, fontFace: BODY, fontSize: size, color: C.ink, paraSpaceAfter: 10, margin: 0, valign: 'top', isTextBox: true });

// ---------- 1. Титул ----------
{
  const s = pres.addSlide();
  s.background = { color: C.dark };
  s.addText('Лабораторная работа № 3 · ООП на языке Java', { x: 0.6, y: 0.7, w: 6.4, h: 0.4, fontFace: BODY, fontSize: 16, color: 'B4B2A9', margin: 0, isTextBox: true });
  s.addText('Мир Вампуса', { x: 0.6, y: 1.5, w: 6.4, h: 1.0, fontFace: HEAD, fontSize: 44, bold: true, color: C.white, margin: 0, isTextBox: true });
  s.addText('Агент, основанный на знаниях: охотник без карты логически выводит, где ямы и Вампус, и добывает золото', { x: 0.6, y: 2.6, w: 6.2, h: 1.3, fontFace: BODY, fontSize: 20, color: 'D3D1C7', margin: 0, isTextBox: true });
  s.addText([
    { text: 'Выполнил: ', options: { color: 'B4B2A9' } }, { text: 'А. А. Минязов, гр. 5130902/40202', options: { color: C.white, breakLine: true } },
    { text: 'Проверил: ', options: { color: 'B4B2A9' } }, { text: 'ст. преп. Д. С. Хасанов', options: { color: C.white, breakLine: true } },
    { text: 'СПбПУ, ИКНК · 2026', options: { color: 'B4B2A9' } },
  ], { x: 0.6, y: 5.4, w: 6.2, h: 1.3, fontFace: BODY, fontSize: 16, margin: 0, paraSpaceAfter: 4, isTextBox: true });
  fit(s, shot('gui_3_won.png'), 7.2, 0.9, 5.6, 5.7);
  s.addNotes('Тема: агент, основанный на знаниях, на примере мира Вампуса. Охотник не видит карту, записывает измерения в базу знаний и логически выводит безопасные комнаты. Java, окно на JavaFX.');
}

// ---------- 2. Задача ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Задача', 'Пещера N×N: Вампус, ямы, золото; охотник знает правила, но не карту');
  bullets(s, [
    'Вампус ест всё в своей комнате и сторожит золото в соседней.',
    'Чтобы взять золото, охотник должен убить Вампуса единственной стрелой.',
    'Приводы: шаг вперёд, поворот на 90°, выстрел вперёд.',
    'Охотник строит базу знаний и делает логический вывод на каждом шаге.',
    'Поле разного размера, ям — пропорционально числу комнат.',
  ], { x: 0.6, y: 1.9, w: 5.8, h: 4.8 });
  const items = [
    ['S', 'Запах (Stench)', 'Вампус здесь или рядом', WUMPUS, C.shockBg],
    ['B', 'Ветерок (Breeze)', 'яма в соседней комнате', C.muted, C.light],
    ['G', 'Блеск (Glitter)', 'золото здесь или рядом', GOLD, C.cheeseBg],
    ['!', 'Удар и крик (Bump, Scream)', 'стена; Вампус убит', HUNTER, C.waterBg],
  ];
  items.forEach(([v, name, desc, col, bg], i) => {
    const y = 1.9 + i * 1.2;
    card(s, 6.8, y, 5.9, 1.0, bg);
    badge(s, 7.0, y + 0.15, 0.7, col, v, C.white, 18);
    s.addText(name, { x: 7.9, y: y + 0.12, w: 4.6, h: 0.4, fontFace: BODY, fontSize: 18, bold: true, color: C.ink, margin: 0, isTextBox: true });
    s.addText(desc, { x: 7.9, y: y + 0.5, w: 4.6, h: 0.4, fontFace: BODY, fontSize: 14, color: C.muted, margin: 0, isTextBox: true });
  });
  s.addText('Измерение — пятёрка (Stench, Breeze, Glitter, Bump, Scream)', { x: 6.8, y: 6.75, w: 5.9, h: 0.35, fontFace: BODY, fontSize: 12, color: C.muted, margin: 0, isTextBox: true });
  s.addNotes('Условие из задания. Охотник знает правила и свои координаты, но не расстановку. Датчики — как в задании. Добавлены действия «взять золото» и «выбраться», без них игру не закончить.');
}

// ---------- 3. База знаний ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Как думает охотник', 'База знаний — набор предложений; TELL записывает, ASK спрашивает');
  const steps = [
    ['1', 'Измерить', 'пять датчиков в текущей комнате'],
    ['2', 'TELL', 'ветерок в 4,2 → P(3,2) ∨ P(4,3) ∨ P(4,1)'],
    ['3', 'ASK', 'для каждой комнаты: яма? Вампус? золото? — нет / может быть / да'],
    ['4', 'Решить', 'золото, выстрел, ближайшая OK-комната или выход'],
  ];
  steps.forEach(([n, name, desc], i) => {
    const x = 0.6 + i * 3.1;
    card(s, x, 1.95, 2.8, 2.3, C.light);
    badge(s, x + 0.2, 2.15, 0.6, HUNTER, n, C.white, 18);
    s.addText(name, { x: x + 0.95, y: 2.2, w: 1.8, h: 0.5, fontFace: BODY, fontSize: 20, bold: true, color: C.ink, margin: 0, isTextBox: true });
    s.addText(desc, { x: x + 0.2, y: 2.95, w: 2.45, h: 1.2, fontFace: BODY, fontSize: 14, color: C.muted, margin: 0, isTextBox: true });
    if (i < 3) {
      s.addText('→', { x: x + 2.8, y: 2.8, w: 0.3, h: 0.5, fontFace: BODY, fontSize: 22, color: HUNTER, align: 'center', margin: 0, isTextBox: true });
    }
  });
  card(s, 0.6, 4.6, 12.1, 1.3, C.pathBg);
  s.addText('KB ⊨ α  ⇔  KB ∧ ¬α невыполнима', { x: 0.9, y: 4.75, w: 11.5, h: 0.6, fontFace: 'Cambria Math', fontSize: 26, italic: true, color: C.okText, margin: 0, isTextBox: true });
  s.addText('«Ямы в 3,2 нет» доказано, если ни одна расстановка, согласная с базой, не кладёт туда яму', { x: 0.9, y: 5.35, w: 11.5, h: 0.45, fontFace: BODY, fontSize: 15, color: '0F6E56', margin: 0, isTextBox: true });
  s.addText([
    { text: 'В базе с начала: ', options: { bold: true } },
    { text: 'Вампус и золото ровно одни · золото рядом с Вампусом · ни Вампус, ни золото не в яме' },
  ], { x: 0.6, y: 6.2, w: 12.1, h: 0.5, fontFace: BODY, fontSize: 15, color: C.ink, margin: 0, isTextBox: true });
  s.addNotes('Факты — переменные да/нет: P — яма, W — Вампус, G — золото в комнате. Правило «ветерок ⇔ яма рядом» при известном измерении даёт либо дизъюнкцию, либо отрицания. Вопрос «безопасна ли комната» — проверка выполнимости.');
}

// ---------- 4. Пример из задания ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Пример из задания', 'Программа повторяет шаги 1–3 задания и объединяет знания разных шагов');
  fit(s, shot('gui_2_step3.png'), 0.6, 1.8, 7.2, 5.3);
  const rows = [
    ['1 · 4,1', 'ничего', 'безопасны 3,1, 4,2', 'иду в 4,2'],
    ['2 · 4,2', 'Breeze', 'яма в 3,2 или 4,3', 'иду в 3,1'],
    ['3 · 3,1', 'Stench', '3,2 OK · яма 4,3 · Вампус 2,1', 'стреляю вверх'],
  ];
  rows.forEach(([n, m, c, d], i) => {
    const y = 1.85 + i * 1.45;
    card(s, 8.1, y, 4.6, 1.3, i === 2 ? C.pathBg : C.light);
    s.addText(`Шаг ${n}: ${m}`, { x: 8.3, y: y + 0.08, w: 4.3, h: 0.4, fontFace: BODY, fontSize: 15, bold: true, color: C.ink, margin: 0, isTextBox: true });
    s.addText(c, { x: 8.3, y: y + 0.47, w: 4.3, h: 0.4, fontFace: BODY, fontSize: 14, color: i === 2 ? C.okText : C.muted, margin: 0, isTextBox: true });
    s.addText('→ ' + d, { x: 8.3, y: y + 0.85, w: 4.3, h: 0.35, fontFace: BODY, fontSize: 14, color: HUNTER, margin: 0, isTextBox: true });
  });
  s.addText('Шаг 3: ветерка в 3,1 нет → яма из шага 2 в 4,3; запаха в 4,2 не было → Вампус не в 3,2, а в 2,1', { x: 8.1, y: 6.25, w: 4.6, h: 0.9, fontFace: BODY, fontSize: 13, color: C.muted, margin: 0, isTextBox: true });
  s.addNotes('В задании в шаге 3 написано «Вампус в 3.1» — по рисунку и логике это 2,1, опечатка. Дальше охотник стреляет, слышит крик, по блеску выводит золото в 2,2, берёт его и выходит: счёт +968.');
}

// ---------- 5. DPLL ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Логический вывод: DPLL', 'Не перебор 2^(3N²) расстановок, а умный поиск');
  bullets(s, [
    'Задаёт значения переменных по одной и сразу выводит следствия из предложений, где остался один вариант.',
    'При противоречии запоминает его причину и откатывается к решению, которое к ней привело (обучение на конфликтах).',
    'Простой DPLL с откатом на шаг назад на пещерах 6×6 не заканчивался — перебирал ямы, не относящиеся к делу.',
    'Одна найденная расстановка отвечает сразу на много вопросов; доказанное запоминается навсегда.',
  ], { x: 0.6, y: 1.9, w: 7.0, h: 4.9 }, 16);
  const stats = [
    ['15 мс', 'целая игра в пещере 6×6', HUNTER],
    ['≈ 140 мс', 'самое долгое действие в пещере 12×12', HUNTER],
    ['21 тыс.', 'предложений в базе 12×12', C.ink],
  ];
  stats.forEach(([v, l, col], i) => {
    s.addText(v, { x: 8.3, y: 1.9 + i * 1.6, w: 4.4, h: 0.8, fontFace: HEAD, fontSize: 38, bold: true, color: col, margin: 0, isTextBox: true });
    s.addText(l, { x: 8.3, y: 2.7 + i * 1.6, w: 4.4, h: 0.5, fontFace: BODY, fontSize: 15, color: C.muted, margin: 0, isTextBox: true });
  });
  s.addNotes('Статус факта: может ли он быть истинным и может ли быть ложным. Оба — «может быть», одно — доказано. Проверено: решатель совпадает с полным перебором на 4500 случайных формулах.');
}

// ---------- 6. Стратегия ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Стратегия охотника', 'Цель выбирается по порядку; ходит только через комнаты, где уже был');
  const list = [
    ['Золото у меня', 'к выходу'],
    ['Вампус убит, золото найдено', 'за золотом'],
    ['Стрелы нет, Вампус жив', 'к выходу: золото не взять'],
    ['Вампус найден (W!)', 'на одну линию и выстрел'],
    ['Есть OK-комната', 'в ближайшую по числу действий'],
    ['Тупик, риск разрешён', 'выстрел наугад или недоказанная яма'],
    ['Иначе', 'к выходу без золота'],
  ];
  list.forEach(([a, b], i) => {
    const y = 1.85 + i * 0.72;
    badge(s, 0.6, y, 0.5, i < 2 ? GOLD : i === 3 ? WUMPUS : i === 5 ? C.muted : HUNTER, String(i + 1), C.white, 14);
    s.addText(a, { x: 1.25, y, w: 3.3, h: 0.5, fontFace: BODY, fontSize: 15, bold: true, color: C.ink, valign: 'middle', margin: 0, isTextBox: true });
    s.addText('→ ' + b, { x: 4.5, y, w: 2.5, h: 0.5, fontFace: BODY, fontSize: 14, color: C.muted, valign: 'middle', margin: 0, isTextBox: true });
  });
  fit(s, shot('gui_5_big.png'), 7.1, 1.8, 5.7, 4.6);
  s.addText('10×10, 20 ям: по блеску и запаху вывел Вампуса в 9,5 и золото в 10,5 — выстрел, золото, выход', { x: 7.1, y: 6.45, w: 5.7, h: 0.7, fontFace: BODY, fontSize: 13, color: C.muted, margin: 0, isTextBox: true });
  s.addNotes('Путь — поиск в ширину по посещённым комнатам с учётом поворотов. В журнале одна строка на решение, сама дорога — несколько действий.');
}

// ---------- 7. Результаты ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Результаты', '1000 случайных пещер каждого размера, 20 % ям');
  s.addChart(pres.charts.BAR, [
    { name: 'Побед с риском', labels: ['4×4', '6×6', '8×8'], values: [70, 45, 28] },
    { name: 'Побед без риска', labels: ['4×4', '6×6', '8×8'], values: [64, 38, 23] },
  ], {
    x: 0.6, y: 1.8, w: 6.6, h: 4.8, barDir: 'col', barGrouping: 'clustered',
    showValue: true, dataLabelPosition: 'outEnd', dataLabelFontSize: 12, dataLabelColor: C.ink, dataLabelFormatCode: '0"%"',
    chartColors: [HUNTER, OK], showLegend: true, legendPos: 'b', legendFontSize: 13, legendColor: C.ink,
    catAxisLabelColor: C.muted, valAxisLabelColor: C.muted, catAxisLabelFontSize: 14, valAxisLabelFontSize: 11,
    valAxisMinVal: 0, valAxisMaxVal: 100, valGridLine: { color: C.line, size: 0.5 }, catGridLine: { style: 'none' },
  });
  const x = 7.6;
  card(s, x, 1.8, 5.1, 1.9, C.pathBg);
  s.addText('0 %', { x: x + 0.3, y: 1.9, w: 4.6, h: 0.9, fontFace: HEAD, fontSize: 44, bold: true, color: OK, margin: 0, isTextBox: true });
  s.addText('гибели без риска: охотник ни разу не ходит в недоказанную опасность', { x: x + 0.3, y: 2.8, w: 4.6, h: 0.8, fontFace: BODY, fontSize: 14, color: C.okText, margin: 0, isTextBox: true });
  bullets(s, [
    'При полной карте проходимы 99 / 99 / 96 % пещер — проигрыши не от ошибок.',
    'На 8×8 ветерок почти везде: в 227 из 231 проигрыша без риска Вампус так и не найден.',
    'Логика даёт только достоверные выводы; оценить, какая комната опаснее, может лишь вероятностный вывод.',
  ], { x, y: 4.0, w: 5.1, h: 3.0 }, 14);
  s.addNotes('С риском гибель 5–7 %, но побед больше. Без риска охотник уходит, когда безопасных комнат нет.');
}

// ---------- 8. Архитектура ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Архитектура', 'Мир — логика — агент — управление — окно; всё кроме окна не зависит от JavaFX');
  const cols = [
    ['model + logic', 'Cave, CaveGenerator, World — мир\nRules — правила → предложения\nKnowledgeBase — TELL / ASK\nDpll — выполнимость', OK, C.pathBg],
    ['agent', 'Hunter — измерить, записать,\nспросить, решить; журнал\nPlanner — путь и повороты\nCellKnowledge, Turn', HUNTER, C.waterBg],
    ['controller + view', 'GameController — кнопки, таймер\nMarks — OK / P? / P! на карте\nCaveForm — проверка ввода\nWumpusApp, CaveCanvas — окно', GOLD, C.cheeseBg],
  ];
  cols.forEach(([name, text, col, bg], i) => {
    const x = 0.6 + i * 4.1;
    card(s, x, 1.9, 3.8, 2.95, bg);
    s.addText(name, { x: x + 0.3, y: 2.1, w: 3.3, h: 0.5, fontFace: HEAD, fontSize: 22, bold: true, color: col, margin: 0, isTextBox: true });
    s.addText(text, { x: x + 0.3, y: 2.75, w: 3.3, h: 2.3, fontFace: BODY, fontSize: 15, color: C.ink, margin: 0, paraSpaceAfter: 6, valign: 'top', isTextBox: true });
  });
  s.addText([
    { text: 'Инкапсуляция: ', options: { bold: true } }, { text: 'охотник не видит пещеру — только измерения; состояние игры меняет только World.execute.', options: { breakLine: true } },
    { text: 'Наследование и полиморфизм: ', options: { bold: true } }, { text: 'WumpusApp → Application, CaveCanvas → Pane (два режима карты), FormDialog<T> → Dialog; sealed FormResult.' },
  ], { x: 0.6, y: 5.2, w: 12.1, h: 1.3, fontFace: BODY, fontSize: 15, color: C.ink, paraSpaceAfter: 8, margin: 0, valign: 'top', isTextBox: true });
  s.addNotes('Охотнику не передаётся объект пещеры — только измерения, как в задании. Логика кнопок проверяется тестами без окна.');
}

// ---------- 9. UML ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'UML-диаграммы', 'Шесть видов диаграмм, построены в PlantUML');
  const list = [
    ['usecase.png', 'Прецеденты'], ['activity_turn.png', 'Деятельность'], ['class_model.png', 'Классы'],
    ['sequence_step.png', 'Взаимодействие'], ['state_hunter.png', 'Состояния'], ['component.png', 'Компоненты'],
  ];
  list.forEach(([file, name], i) => {
    const col = i % 3, row = Math.floor(i / 3);
    const x = 0.6 + col * 4.1, y = 1.8 + row * 2.7;
    card(s, x, y, 3.8, 2.45, C.light);
    fit(s, uml(file), x + 0.15, y + 0.12, 3.5, 1.8);
    s.addText(name, { x: x + 0.15, y: y + 1.98, w: 3.5, h: 0.38, fontFace: BODY, fontSize: 15, bold: true, color: C.ink, align: 'center', margin: 0, isTextBox: true });
  });
  s.addNotes('В отчёте девять диаграмм: прецеденты; деятельность — шаг охотника и вывод статуса факта; классы — мир и логика, охотник и окно; последовательность — кнопка «Шаг»; состояния — игра и цель охотника; компоненты.');
}

// ---------- 10. Демонстрация ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Демонстрация', 'Одна пещера 6×6: с риском и без');
  fit(s, shot('gui_6_death.png'), 0.6, 1.8, 5.9, 4.3);
  fit(s, shot('gui_7_cautious.png'), 6.8, 1.8, 5.9, 4.3);
  s.addText('С риском: выстрел наугад убил Вампуса, затем шаг в недоказанную яму — гибель', { x: 0.6, y: 6.2, w: 5.9, h: 0.7, fontFace: BODY, fontSize: 15, color: WUMPUS, margin: 0, isTextBox: true });
  s.addText('Без риска: обошёл все доказанно безопасные комнаты и вышел живым без золота', { x: 6.8, y: 6.2, w: 5.9, h: 0.7, fontFace: BODY, fontSize: 15, color: OK, bold: true, margin: 0, isTextBox: true });
  s.addNotes('Живая демонстрация: «Пример из задания», три раза «Шаг» — журнал как в задании. Снять «показывать правду» — видно, что охотник карту не видит. «Новая пещера» с ошибками — окно подсвечивает поля и не закрывается.');
}

// ---------- 11. Итоги ----------
{
  const s = pres.addSlide();
  s.background = { color: C.dark };
  s.addText('Итоги', { x: 0.6, y: 0.6, w: 12, h: 0.9, fontFace: HEAD, fontSize: 40, bold: true, color: C.white, margin: 0, isTextBox: true });
  const res = [
    ['База знаний', 'правила и измерения — предложения; TELL и ASK на каждом шаге'],
    ['Вывод', 'DPLL с обучением на конфликтах; пример задания повторён шаг в шаг'],
    ['Пещера', 'от 4×4 до 12×12, ям — доля от числа комнат, случайно по seed'],
    ['ООП, UML, тесты', 'пять пакетов; шесть видов UML; 169 автотестов'],
  ];
  res.forEach(([name, text], i) => {
    const y = 1.9 + i * 1.2;
    badge(s, 0.6, y, 0.6, OK, String(i + 1), C.white, 18);
    s.addText(name, { x: 1.45, y: y - 0.02, w: 2.6, h: 0.6, fontFace: BODY, fontSize: 22, bold: true, color: C.white, margin: 0, valign: 'middle', isTextBox: true });
    s.addText(text, { x: 4.1, y: y - 0.02, w: 8.6, h: 0.6, fontFace: BODY, fontSize: 18, color: 'D3D1C7', margin: 0, valign: 'middle', isTextBox: true });
  });
  s.addText('Спасибо за внимание', { x: 0.6, y: 6.6, w: 12, h: 0.5, fontFace: BODY, fontSize: 18, color: 'B4B2A9', margin: 0, isTextBox: true });
  s.addNotes('Главное: охотник действует только по логическому выводу из базы знаний, как требует задание; всё доказанное проверено тестами на совпадение с настоящей пещерой.');
}

pres.writeFile({ fileName: OUT }).then(f => console.log('готово:', f));
