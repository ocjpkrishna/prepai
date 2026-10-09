import katex from 'katex';

export interface Measure {
	w: number;
	h: number;
	html?: string;
}

export type TextMeasurer = (kind: 'text' | 'math', content: string, fontSize: number, maxWidth: number, bold?: boolean) => Measure;

const LINE_HEIGHT = 1.35;
const TEXT_CHAR_WIDTH = 0.52;
const MATH_CHAR_WIDTH = 0.56;

export function mathHtml(latex: string): string {
	return katex.renderToString(latex, { throwOnError: false, trust: false, displayMode: false, output: 'html' });
}

export function estimateMeasurer(): TextMeasurer {
	return (kind, content, fontSize, maxWidth, bold) => {
		if (kind === 'math') {
			return estimateMath(content, fontSize);
		}
		return estimateText(content, fontSize * (bold ? 1.12 : 1), maxWidth);
	};
}

function estimateMath(content: string, fontSize: number): Measure {
	const visible = content.replace(/\\[a-zA-Z]+/g, 'x').replace(/[{}]/g, '');
	const tall = /\\(frac|sqrt|int|sum)/.test(content);
	return { w: visible.length * fontSize * MATH_CHAR_WIDTH, h: fontSize * (tall ? 2.3 : 1.5), html: mathHtml(content) };
}

function estimateText(content: string, fontSize: number, maxWidth: number): Measure {
	const charWidth = fontSize * TEXT_CHAR_WIDTH;
	let lines = 1;
	let used = 0;
	for (const word of content.split(/\s+/)) {
		const width = (word.length + 1) * charWidth;
		if (used + width > maxWidth && used > 0) {
			lines++;
			used = 0;
		}
		used += width;
	}
	const w = lines === 1 ? Math.min(maxWidth, used) : maxWidth;
	return { w, h: lines * fontSize * LINE_HEIGHT };
}

export function domMeasurer(doc: Document): TextMeasurer {
	const host = doc.createElement('div');
	host.style.cssText = 'position:absolute;left:-10000px;top:0;visibility:hidden;pointer-events:none';
	doc.body.appendChild(host);
	return (kind, content, fontSize, maxWidth, bold) => {
		const el = doc.createElement('div');
		el.style.fontWeight = bold ? '700' : '400';
		el.className = kind === 'math' ? 'wb-math' : 'wb-text';
		el.style.fontSize = `${fontSize}px`;
		const html = kind === 'math' ? mathHtml(content) : undefined;
		if (html) {
			el.innerHTML = html;
		} else {
			el.style.maxWidth = `${maxWidth}px`;
			el.textContent = content;
		}
		host.appendChild(el);
		const rect = el.getBoundingClientRect();
		host.removeChild(el);
		return { w: Math.ceil(rect.width) + 2, h: Math.ceil(rect.height) + 2, html };
	};
}
