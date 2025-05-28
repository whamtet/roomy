const format2 = s => s < 10 ? '0' + s : s;
const $ = x => document.querySelector(x);
const $$ = x => Array.from(document.querySelectorAll(x));
const $m = m => Object.keys(m).map(k => $('#' + k).value = m[k]);

const maxInnerHTML = (selector, x) => {
    const y = Number($(selector).innerHTML);
    $(selector).innerHTML = format2(Math.max(x, y));
};
const maxValue = (selector, x) => {
    const y = Number($(selector).value);
    $(selector).value = format2(Math.max(x, y));
};
const minInnerHTML = (selector, x) => {
    const y = Number($(selector).innerHTML);
    $(selector).innerHTML = format2(Math.min(x, y));
};
const minValue = (selector, x) => {
    const y = Number($(selector).value);
    $(selector).value = format2(Math.min(x, y));
};

const serialize = date => {
    if (date.getFullYear) {
        return `${format2(date.getMonth() + 1)}/${format2(date.getDate())}/${date.getFullYear()}`;
    } else {
        return '';
    }
}
const deserialize = dateStr => {
    const [month, date, year] = dateStr.split('/');
    const d = new Date();
    d.setMonth(Number(month) - 1);
    d.setDate(Number(date));
    d.setFullYear(Number(year));

    return d;
}

const addDatepicker = (selector, val, onChange, minStr) => {
    const min = minStr ? deserialize(minStr) : undefined;
    new Datepicker(selector, {serialize, min, onChange});
    $(selector).value = val;
};

const disable = x => $$(x).map(el => el.disabled = true);
const enable = x => $$(x).map(el => el.disabled = false);
const show = x => $$(x).map(el => el.classList.remove('hidden'));
const hide = x => $$(x).map(el => el.classList.add('hidden'));

const timeVal = suffix => {
    let h = Number($(`#hour${suffix}`).innerHTML);
    const m = Number($(`#minute${suffix}`).innerHTML);

    const ampm = $(`#ampm${suffix}`);
    if (ampm) {
        if (ampm.innerText.trim() === 'AM') {
            if (h === 12) {
                h = 0;
            }
        } else {
            h += 12;
        }
    }

    return h * 60 + m;
}

function tabIndex(i) {
    $('#tab-index').value = i;
}

function focusId(id) {
    document.getElementById(id).focus();
}
