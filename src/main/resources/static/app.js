// Tema
(function () {
    const root = document.documentElement;
    const key = 'se_theme';
    const saved = localStorage.getItem(key);
    if (saved === 'dark') root.setAttribute('data-theme', 'dark');

    const sw = document.getElementById('themeSwitch');
    if (sw) {
        sw.checked = root.getAttribute('data-theme') === 'dark';
        sw.addEventListener('change', (e) => {
            if (e.target.checked) { root.setAttribute('data-theme', 'dark'); localStorage.setItem(key, 'dark'); }
            else { root.removeAttribute('data-theme'); localStorage.setItem(key, 'light'); }
        });
    }
})();

// Compartir
(function () {
    const shareBtn = document.getElementById('shareBtn');
    shareBtn?.addEventListener('click', async () => {
        const url = window.location.href;
        try {
            if (navigator.share) await navigator.share({ title: document.title, url });
            else {
                await navigator.clipboard.writeText(url);
                shareBtn.textContent = '✅';
                setTimeout(() => shareBtn.textContent = '🔗', 900);
            }
        } catch (_) { /* noop */ }
    });
})();

// Helpers
function peopleFromDatalist() {
    return Array.from(document.querySelectorAll('#peoplelist option')).map(o => o.value).filter(Boolean);
}
function norm(s) { return (s || '').normalize('NFD').replace(/\p{M}+/gu, '').toLowerCase().trim(); }

// Autocomplete TAB multi-token (payers y participants)
function completeMultiWithTab(input) {
    if (!input) return;
    input.addEventListener('keydown', (e) => {
        if (e.key !== 'Tab') return;
        const val = input.value || '';
        const people = peopleFromDatalist();
        const parts = val.split(',');
        const before = parts.slice(0, -1).map(s => s.trim()).filter(Boolean);
        const lastRaw = (parts[parts.length - 1] || '').trim();
        const m = people.find(p => norm(p).startsWith(norm(lastRaw)));
        if (m) {
            e.preventDefault();
            const dedup = [...before];
            if (!dedup.some(x => norm(x) === norm(m))) dedup.push(m);
            input.value = dedup.join(',') + ',';
        }
    });
}

// Wire up form
(function () {
    const expenseForm = document.getElementById('expenseForm');
    const payersInput = document.getElementById('payersInput');
    const participantsInput = document.getElementById('participantsInput');

    completeMultiWithTab(payersInput);
    completeMultiWithTab(participantsInput);

    const btnAll = document.getElementById('btnAll');
    const btnNone = document.getElementById('btnNone');
    const btnPlusPayers = document.getElementById('btnPlusPayers');
    const btnPayersAll = document.getElementById('btnPayersAll');
    const btnPayersNone = document.getElementById('btnPayersNone');

    btnAll?.addEventListener('click', () => {
        participantsInput.value = peopleFromDatalist().join(',');
        participantsInput.dispatchEvent(new Event('input'));
    });
    btnNone?.addEventListener('click', () => {
        participantsInput.value = '';
        participantsInput.dispatchEvent(new Event('input'));
    });
    btnPlusPayers?.addEventListener('click', () => {
        const payers = (payersInput.value || '').split(',').map(s => s.trim()).filter(Boolean);
        if (payers.length === 0) return;
        const list = peopleFromDatalist();
        const curr = (participantsInput.value || '').split(',').map(s => s.trim()).filter(Boolean);
        for (const p of payers) {
            const match = list.find(x => norm(x) === norm(p));
            if (match && !curr.some(y => norm(y) === norm(match))) curr.push(match);
        }
        participantsInput.value = curr.join(',');
        participantsInput.dispatchEvent(new Event('input'));
    });
    btnPayersAll?.addEventListener('click', () => {
        payersInput.value = peopleFromDatalist().join(',');
        payersInput.dispatchEvent(new Event('input'));
    });
    btnPayersNone?.addEventListener('click', () => {
        payersInput.value = '';
        payersInput.dispatchEvent(new Event('input'));
    });

    // Antes de enviar: convertir "participants" CSV en inputs múltiples name=participants
    expenseForm?.addEventListener('submit', (e) => {
        // limpiar previos
        expenseForm.querySelectorAll('input[name="participants"]').forEach(n => n.remove());
        const parts = (participantsInput.value || '').split(',').map(s => s.trim()).filter(Boolean);
        for (const p of parts) {
            const hidden = document.createElement('input');
            hidden.type = 'hidden';
            hidden.name = 'participants';
            hidden.value = p;
            expenseForm.appendChild(hidden);
        }
    });
})();
