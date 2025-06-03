const onChange = () => {
    htmx.trigger('#date-trigger', 'click');
};

new Datepicker('#datepicker', {serialize, onChange});
$('#datepicker').value = serialize(new Date());

const initTrigger = () => {
    if (!$('#room-info')) {
        onChange();
        setTimeout(initTrigger, 100);
    }
};
initTrigger();

const andJoin = s => {
    if (s.length > 1) {
        return s.slice(0, -1).join(', ') + ' and ' + s[s.length - 1];
    } else {
        return s[0];
    }
};

function checkSwap(repeat, interval, fullWeek) {
    if (interval === 1) {
        if (repeat === 'daily' && !fullWeek || repeat === 'weekly' && fullWeek) {
            $('#swap').click();
        }
    }
}

const getUntil = () => {
    const limitType = $('#limit-type').value;

    // update
    if (limitType === 'date') {
        show('#end-date')
    } else {
        hide('#end-date');
    }

    if (limitType === 'forever') {
        return '.';
    } else if (limitType === 'date') {
        return ` until ${$('#end-date').value}.`;
    } else if (limitType === '1') {
        return ' 1 time.';
    } else {
        return ` ${limitType} times.`;
    }
}

function refreshSummary() {

    const weekdays = $$('.weekday').map(x => x.value);
    const until = getUntil();
    const interval = Number($('#interval').value);
    const repeat = $('#repeat-stored').value;
    const fullWeek = weekdays.length === 7;
    let summary;

    $('#days').value = JSON.stringify(weekdays);
    checkSwap(repeat, interval, fullWeek);

    if (repeat === 'weekly') {
        if (interval === 2) {
            summary = fullWeek ? 'Repeats every other week' + until :
                'Repeats every other week on ' + andJoin(weekdays) + until;
        } else if (interval > 2) {
            summary = fullWeek ? `Repeats every ${interval} weeks` + until :
                `Repeats every ${interval} weeks on ` + andJoin(weekdays) + until;
        } else {
            // cannot be fullWeek
            summary = `Repeats every week on ` + andJoin(weekdays) + until;
        }
    } else { // daily
        if (interval === 2) {
            summary = 'Repeats every other day' + until;
        } else if (interval > 1) {
            summary = `Repeats every ${interval} days` + until;
        } else {
            summary = 'Repeats every day' + until;
        }
    }
    $('#repeat-summary').innerHTML = summary;

}

function clickDay(e) {
    if (e.target.classList.contains('weekday')) {
        // unselecting
        if ($$('.weekday').length > 1) {
            e.target.classList.remove('weekday');
            refreshSummary();
        }
    } else {
        e.target.classList.add('weekday');
        refreshSummary();
    }
}

const ordinal_ = x => x === 1 ? 'st' : x === 2 ? 'nd' : x === 3 ? 'rd' : 'th';
const ordinal = x => ordinal_(x % 10);

const some = (f, s) => {
    for (x of s) {
        const y = f(x);
        if (y) {
            return y;
        }
    }
};
const checked = x => x.checked ? x.value : undefined;

function refreshSummaryMonthly() {
    const until = getUntil();
    const interval = Number($('#interval').value);
    const date = Number($('#date').value);
    const week = $('#week').value;
    const weekday = $('#weekday').value;

    const datePattern = some(checked, $$('.date-pattern'));
    let on;
    if (datePattern === 'date') {
        on = `on the ${date}${ordinal(date)}`
    } else if (datePattern === 'week') {
        on = `on the ${week}`;
    } else {
        on = `on the last ${weekday}`;
    }

    if (interval === 2) {
        summary = 'Repeats every other month '
    } else if (interval > 2) {
        summary = `Repeats every ${interval} months `;
    } else {
        summary = 'Repeats every month ';
    }

    $('#repeat-summary').innerHTML = summary + on + until;

}

function refreshSummaryYearly() {
    const until = $('#limit-type').value === 'forever' ? '.' : ` until ${$('#limit-type').value}.`;
    const date = Number($('#date').value);
    const week = $('#week').value;
    const weekday = $('#weekday').value;
    const month = $('#month').value;

    const datePattern = some(checked, $$('.date-pattern'));
    let on;
    if (datePattern === 'date') {
        on = `on ${month} ${date}`
    } else if (datePattern === 'week') {
        on = `on the ${week} of ${month}`;
    } else {
        on = `on the last ${weekday} of ${month}`;
    }

    const summary = 'Repeats each year ';
    $('#repeat-summary').innerHTML = summary + on + until;

}

const checkCrossing = () => {
    if (timeVal('2') <= timeVal('')) {
        show('#crossing');
        disable('#proceed-single');
    } else {
        hide('#crossing');
        enable('#proceed-single');
    }
};

// used in time_select.clj
const mod12_ = x => x === 0 ? 12 : ((x - 1) % 12) + 1;
const mod12 = x => mod12_(Number(x));

function submitBooking() {
    const form = $('#booking-save');
    if (form.checkValidity()) {
        const bookingDetails = $('#booking-details').value.trim();
        if (bookingDetails || confirm('Leave body empty?')) {
            $('#book-submit').click();
        }
    } else {
        form.reportValidity();
    }
}
