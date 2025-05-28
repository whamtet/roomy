const onChangeMultiday = times => {
    const t1 = times[0], t2 = times[times.length - 1];
    const start = new Date(Math.min(t1, t2));
    const end = new Date(Math.max(t1, t2));

    if (t1) {
        hide('.multi-message');
        if (end <= start) {
            show('#time-cross');
            disable('#proceed-multiple')
        } else {
            enable('#proceed-multiple');
        }
    }
};

const sm = date => {
    const dateStr = `${format2(date.getMonth() + 1)}/${format2(date.getDate())}/${date.getFullYear()}`;
    return `${dateStr} ${format2(date.getHours())}:${format2(date.getMinutes())}`;
}
const serializeMultiday = x => x.length ? x.map(sm).join() : sm(x);
