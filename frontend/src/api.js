// Accesso alle API REST del backend con fetch nativo.
// Le richieste sono sulla stessa origine, quindi il browser invia da solo il cookie di sessione (JSESSIONID).

/** Errore restituito dal backend: status HTTP + ProblemDetail (title, detail, ed eventualmente errori[]). */
export class ApiError extends Error {
  constructor(status, problema) {
    super(problema?.detail || `Errore ${status}`);
    this.status = status;
    this.problema = problema;
  }

  /** Errori di validazione per campo: { testo: "...", voto: "..." } (dal 400 di ApiExceptionHandler). */
  get erroriCampi() {
    const errori = {};
    (this.problema?.errori || []).forEach((e) => { errori[e.campo] = e.messaggio; });
    return errori;
  }
}

/** Legge un cookie dal browser (serve per il token CSRF). */
export function leggiCookie(nome) {
  const riga = document.cookie.split('; ').find((c) => c.startsWith(nome + '='));
  return riga ? decodeURIComponent(riga.substring(nome.length + 1)) : null;
}

async function richiesta(metodo, url, corpo) {
  const headers = { Accept: 'application/json' };
  if (corpo !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  // CSRF: Spring Security mette il token nel cookie XSRF-TOKEN e si aspetta di ritrovarlo in questo header
  // in ogni richiesta che modifica dati. Un sito esterno non può leggere il cookie, quindi non può imitarlo.
  if (metodo !== 'GET') {
    headers['X-XSRF-TOKEN'] = leggiCookie('XSRF-TOKEN');
  }

  const risposta = await fetch(url, {
    method: metodo,
    headers,
    credentials: 'same-origin',
    body: corpo !== undefined ? JSON.stringify(corpo) : undefined,
  });

  if (risposta.status === 204) {
    return null; // DELETE riuscita: nessun contenuto
  }
  const dati = await risposta.json().catch(() => null);
  if (!risposta.ok) {
    throw new ApiError(risposta.status, dati);
  }
  return dati;
}

export const api = {
  get: (url) => richiesta('GET', url),
  post: (url, corpo) => richiesta('POST', url, corpo),
  put: (url, corpo) => richiesta('PUT', url, corpo),
  delete: (url) => richiesta('DELETE', url),
};

/** Messaggio leggibile per gli errori più comuni delle API. */
export function messaggioErrore(errore) {
  if (!(errore instanceof ApiError)) {
    return 'Impossibile contattare il server. Riprova.';
  }
  switch (errore.status) {
    case 401: return 'Devi accedere per eseguire questa operazione.';
    case 403: return errore.problema?.detail || 'Non hai i permessi per questa operazione.';
    case 404: return errore.problema?.detail || 'Risorsa non trovata.';
    case 409: return errore.problema?.detail || 'Operazione in conflitto con i dati esistenti.';
    default: return errore.message;
  }
}
