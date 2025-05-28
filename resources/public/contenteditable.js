const getContent = el => {
    el = el.cloneNode(true);
    el.querySelectorAll('.editable-link').forEach(a => {
        a.outerHTML = a.dataset.href;
    });
    return el.innerText.trim();
};

const hrefText = href => {
    if (href.includes('zoom.us/')) {
        return 'Zoom';
    }
    const match = href.match(/https?:\/\/([^\?\/\s]+)/);
    return match ? match[1] : 'Link';
};

const hrefEditable_ = href => (
    `<span 
       contenteditable="false"
       class="editable-link cursor-pointer relative text-blue-400"
       data-href="${href}"
     >${hrefText(href)}<span
         class="absolute invisible z-10 p-2 bg-white border"
         onclick="window.open('${href}')"
       >${href}</span>
     </span>`
);

const hrefEditable = (s, i) => i % 2 === 1 ? hrefEditable_(s) : s;

const splitRegex = s => {
    const split = [];
    let match = s.match(/https?:\/\/\S+/);
    while (match) {
        split.push(s.substring(0, match.index));
        split.push(match[0]);
        s = s.substring(match.index + match[0].length);
        match = s.match(/http\S+/);
    }
    split.push(s); // possible last fragment
    return split;
};

const formatLinks = s => splitRegex(s).map(hrefEditable).join('');

function formatContentEditable(uuid) {
    const disp = document.getElementById(uuid);
    const storage = document.getElementById(uuid + '-data');

    storage.value = getContent(disp);
    // copy back again
    disp.innerHTML = formatLinks(storage.value) + '&nbsp;';
}
