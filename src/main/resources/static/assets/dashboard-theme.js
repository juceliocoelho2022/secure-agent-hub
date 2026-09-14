(() => {
    const STORAGE_KEY = 'secure-agent-hub-theme';
    const root = document.documentElement;

    function normalizeTheme(value) {
        return value === 'light' ? 'light' : 'dark';
    }

    function applyTheme(theme) {
        const selected = normalizeTheme(theme);
        root.setAttribute('data-theme', selected);
        localStorage.setItem(STORAGE_KEY, selected);

        const button = document.getElementById('themeToggle');
        if (button) {
            const nextTheme = selected === 'dark' ? 'light' : 'dark';
            button.textContent = selected === 'dark' ? '☀ Light' : '☾ Dark';
            button.setAttribute('aria-label', `Switch to ${nextTheme} theme`);
            button.setAttribute('title', `Switch to ${nextTheme} theme`);
            button.setAttribute('aria-pressed', selected === 'light' ? 'true' : 'false');
        }
    }

    const storedTheme = localStorage.getItem(STORAGE_KEY);
    applyTheme(storedTheme || 'dark');

    document.addEventListener('DOMContentLoaded', () => {
        const button = document.getElementById('themeToggle');
        if (!button) return;

        applyTheme(root.getAttribute('data-theme'));
        button.addEventListener('click', () => {
            const current = normalizeTheme(root.getAttribute('data-theme'));
            applyTheme(current === 'dark' ? 'light' : 'dark');
        });
    });
})();
