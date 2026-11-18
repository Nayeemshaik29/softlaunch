// Account deletion page. No libraries, no trackers: it only talks to our own API.
(() => {
  'use strict';
  const API = '/api/users/auth/deletion-requests';
  const $ = (id) => document.getElementById(id);

  function show(step) {
    document.querySelectorAll('[data-step]').forEach((s) => { s.hidden = s.id !== step; });
  }

  async function post(path, body) {
    try {
      const res = await fetch(API + path, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
        credentials: 'omit',
      });
      let data = {};
      try { data = await res.json(); } catch { /* 204 / empty body */ }
      return { status: res.status, data };
    } catch {
      return { status: 0, data: {} };   // offline / server unreachable
    }
  }

  function commonError(status) {
    if (status === 429) return 'Too many tries. Please wait a minute and try again.';
    if (status === 503) return 'We couldn’t do that right now. Please try again in a minute.';
    if (status === 0) return 'Can’t reach SoftLaunch. Check your connection and try again.';
    return 'Something went wrong. Please try again.';
  }

  // The token sits after "#" in the emailed link: browsers never send that part to a server.
  // Read it once, keep it only in memory, and remove it from the address bar and history.
  const token = new URLSearchParams(location.hash.slice(1)).get('token');
  if (token) {
    history.replaceState(null, '', location.pathname);
    show('confirm');
  } else {
    show('ask');
  }

  $('ask-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = $('email').value.trim();
    const error = $('ask-error');
    const button = e.submitter || $('ask-form').querySelector('button');
    error.textContent = '';
    if (!$('email').checkValidity() || !email) {
      error.textContent = 'Please enter a valid email address.';
      return;
    }
    button.disabled = true;
    const { status, data } = await post('', { email });
    button.disabled = false;
    if (status === 202) {
      if (data.message) $('sent-message').textContent = data.message;
      show('sent');
    } else if (status === 400) {
      error.textContent = 'Please enter a valid email address.';
    } else {
      error.textContent = commonError(status);
    }
  });

  $('understand').addEventListener('change', (e) => { $('delete').disabled = !e.target.checked; });

  $('delete').addEventListener('click', async () => {
    const error = $('confirm-error');
    error.textContent = '';
    $('delete').disabled = true;
    const { status } = await post('/confirm', { token });
    if (status === 204) {
      show('done');
    } else if (status === 400) {
      show('expired');
    } else {
      error.textContent = commonError(status);
      $('delete').disabled = !$('understand').checked;
    }
  });

  $('again').addEventListener('click', () => show('ask'));
  // A second email link opened in this same tab only changes the "#..." part: start over with it.
  window.addEventListener('hashchange', () => location.reload());
})();
