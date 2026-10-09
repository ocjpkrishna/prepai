const UNITS: Record<string, [string, string]> = {
	m: ['metres', 'metre'], km: ['kilometres', 'kilometre'], cm: ['centimetres', 'centimetre'], mm: ['millimetres', 'millimetre'],
	s: ['seconds', 'second'], ms: ['milliseconds', 'millisecond'], min: ['minutes', 'minute'], h: ['hours', 'hour'],
	kg: ['kilograms', 'kilogram'], mg: ['milligrams', 'milligram'],
	N: ['newtons', 'newton'], J: ['joules', 'joule'], kJ: ['kilojoules', 'kilojoule'], W: ['watts', 'watt'], kW: ['kilowatts', 'kilowatt'],
	Pa: ['pascals', 'pascal'], Hz: ['hertz', 'hertz'], kHz: ['kilohertz', 'kilohertz'], MHz: ['megahertz', 'megahertz'],
	V: ['volts', 'volt'], mol: ['moles', 'mole'], K: ['kelvin', 'kelvin'], L: ['litres', 'litre'], eV: ['electron volts', 'electron volt'],
};

const SYMBOLS: [RegExp, string][] = [
	[/⇒|=>/g, ' which gives '],
	[/→|->/g, ' gives '],
	[/≈/g, ' is approximately '],
	[/≠/g, ' is not equal to '],
	[/≤/g, ' is less than or equal to '],
	[/≥/g, ' is greater than or equal to '],
	[/√\s*\(([^)]+)\)/g, ' square root of $1 '],
	[/√\s*(\w+)/g, ' square root of $1 '],
	[/√/g, ' square root of '],
	[/×|·/g, ' times '],
	[/÷/g, ' divided by '],
	[/π/g, ' pi '], [/θ/g, ' theta '], [/Δ/g, ' delta '], [/μ/g, ' mu '], [/λ/g, ' lambda '], [/ω/g, ' omega '],
	[/α/g, ' alpha '], [/β/g, ' beta '], [/γ/g, ' gamma '], [/ρ/g, ' rho '], [/σ/g, ' sigma '], [/φ/g, ' phi '], [/Ω/g, ' ohms '],
];

const unitKeys = Object.keys(UNITS).sort((a, b) => b.length - a.length).join('|');
const POW = '(?:\\^?[23]|[²³])?';
const UNIT = `(?:${unitKeys})${POW}`;
const COMPOUND = `${UNIT}(?:\\s*[/·]\\s*${UNIT})*`;
const WITH_NUMBER = new RegExp(`(\\d[\\d,]*(?:\\.\\d+)?)\\s*(${COMPOUND})(?![A-Za-z²³])`, 'g');
const SLASH_ONLY = new RegExp(`(?<![A-Za-z])(${UNIT}\\s*/\\s*${UNIT}(?:\\s*/\\s*${UNIT})*)(?![A-Za-z²³])`, 'g');

function powerWord(pow: string | undefined): string {
	if (!pow) {
		return '';
	}
	return /3|³/.test(pow) ? ' cubed' : ' squared';
}

function unitWord(part: string, plural: boolean): string {
	const m = /^([A-Za-z]+)(\^?[23]|[²³])?$/.exec(part.trim());
	const unit = m ? UNITS[m[1]] : undefined;
	if (!m || !unit) {
		return part;
	}
	return (plural ? unit[0] : unit[1]) + powerWord(m[2]);
}

export function speakUnits(expr: string, plural = true): string {
	const [top, ...bottom] = expr.split('/').map((s) => s.trim());
	const numerator = top.split('·').map((p, i) => unitWord(p, plural && i === 0)).join(' ');
	const denominator = bottom.map((p) => unitWord(p, false));
	return [numerator, ...denominator].join(' per ');
}

function simplify(text: string): string {
	return text
		.replace(/\\text\{([^}]*)\}/g, '$1')
		.replace(/\\frac\{([^}]*)\}\{([^}]*)\}/g, '$1 over $2')
		.replace(/\\sqrt\{([^}]*)\}/g, 'square root of $1')
		.replace(/\\times/g, ' times ')
		.replace(/\\(?:Rightarrow|implies)/g, ' which gives ')
		.replace(/\\(?:cdot)/g, ' times ')
		.replace(/\\,|\;|\\!/g, ' ');
}

function numbersAndUnits(text: string): string {
	return text
		.replace(/(\d)g\b/g, (_m, n: string) => `${n} ${n === '1' ? 'gram' : 'grams'}`)
		.replace(WITH_NUMBER, (_m, num: string, expr: string) => `${num} ${speakUnits(expr, num.replace(/,/g, '') !== '1')}`)
		.replace(SLASH_ONLY, (_m, expr: string) => speakUnits(expr));
}

function powersAndSubscripts(text: string): string {
	return text
		.replace(/\^\{?2\}?|²/g, ' squared')
		.replace(/\^\{?3\}?|³/g, ' cubed')
		.replace(/\^\{?(-?\w+)\}?/g, ' to the power $1')
		.replace(/\b([A-Za-z])_\{?(\w+)\}?/g, '$1 sub $2');
}

function operators(text: string): string {
	return text
		.replace(/(\d)\s*°\s*C\b/g, '$1 degrees Celsius')
		.replace(/(\d)\s*°/g, '$1 degrees')
		.replace(/(\d)\s*%/g, '$1 percent')
		.replace(/(\d)\s*\/\s*(\d)/g, '$1 over $2')
		.replace(/(?<=\d)\s*[−–-]\s*(?=\d)/g, ' minus ')
		.replace(/(?<=\s)[−–]\s*(?=\d)/g, 'minus ')
		.replace(/\s=\s|=/g, ' equals ')
		.replace(/\+/g, ' plus ')
		.replace(/(\d)([A-Za-z])\b/g, '$1 $2');
}

export function normalizeForSpeech(text: string): string {
	let out = simplify(text);
	for (const [pattern, spoken] of SYMBOLS) {
		out = out.replace(pattern, spoken);
	}
	out = numbersAndUnits(out);
	out = powersAndSubscripts(out);
	out = operators(out);
	return out.replace(/\s+([,.;:!?)])/g, '$1').replace(/\(\s+/g, '(').replace(/\s{2,}/g, ' ').trim();
}
