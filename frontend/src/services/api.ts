import axios, { AxiosError } from 'axios';

/*
 * UNICA istanza axios dell'applicazione: la baseURL si scrive una volta sola e tutti i servizi la usano.
 * Le richieste sono sulla stessa origine del backend (in sviluppo grazie al proxy di Vite), quindi il
 * browser invia da solo il cookie di sessione JSESSIONID: è la stessa sessione delle pagine Thymeleaf.
 *
 * CSRF: Spring Security mette il token nel cookie XSRF-TOKEN e si aspetta di ritrovarlo nell'header
 * X-XSRF-TOKEN di ogni richiesta che modifica dati. axios lo fa da solo: per le richieste sulla stessa
 * origine legge il cookie xsrfCookieName e lo copia nell'header xsrfHeaderName (qui scritti per chiarezza).
 */
const api = axios.create({
  baseURL: '/api',
  headers: { Accept: 'application/json' },
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
  withCredentials: true, // invia il cookie di sessione anche se il frontend gira su un'altra origine (CORS)
});

export default api;

/** Corpo degli errori del backend: ProblemDetail (RFC 7807) prodotto da ApiExceptionHandler. */
interface ProblemDetail {
  title?: string;
  detail?: string;
  errori?: { campo: string; messaggio: string }[];
}

/**
 * Errore "leggibile" che i servizi passano ai componenti: status HTTP, messaggio da mostrare e,
 * per i 400 di validazione, i messaggi per campo ({ testo: "...", voto: "..." }).
 */
export class ApiError extends Error {
  status: number;
  erroriCampi: Record<string, string>;

  constructor(status: number, messaggio: string, erroriCampi: Record<string, string> = {}) {
    super(messaggio);
    this.status = status;
    this.erroriCampi = erroriCampi;
  }
}

/**
 * Trasforma qualunque errore di axios in un ApiError con un messaggio comprensibile.
 * axios, a differenza di fetch, lancia un'eccezione per ogni risposta 4xx/5xx: basta un try/catch.
 */
export function toApiError(errore: unknown): ApiError {
  if (!axios.isAxiosError(errore) || !errore.response) {
    return new ApiError(0, 'Impossibile contattare il server. Riprova.');
  }
  const { status, data } = (errore as AxiosError<ProblemDetail>).response!;
  const erroriCampi: Record<string, string> = {};
  data?.errori?.forEach((e) => { erroriCampi[e.campo] = e.messaggio; });

  switch (status) {
    case 401: return new ApiError(401, 'Devi accedere per eseguire questa operazione.');
    case 403: return new ApiError(403, data?.detail ?? 'Non hai i permessi per questa operazione.');
    case 404: return new ApiError(404, data?.detail ?? 'Risorsa non trovata.');
    case 409: return new ApiError(409, data?.detail ?? 'Operazione in conflitto con i dati esistenti.');
    default: return new ApiError(status, data?.detail ?? `Errore ${status}`, erroriCampi);
  }
}
