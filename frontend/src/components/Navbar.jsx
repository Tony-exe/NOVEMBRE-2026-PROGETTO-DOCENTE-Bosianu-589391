import { leggiCookie } from '../api.js';

/**
 * Stessa navbar delle pagine Thymeleaf, così React e Thymeleaf sembrano un'unica applicazione.
 * I link portano alle pagine Thymeleaf con normali <a href> (navigazione completa, non del router React).
 */
export default function Navbar({ utente }) {
  const isAdmin = utente?.ruolo === 'ADMIN';

  return (
    <nav className="navbar navbar-dark bg-dark mb-4">
      <div className="container">
        <div className="d-flex align-items-center gap-3">
          <a className="navbar-brand" href="/">🎬 Festival</a>
          <a className="nav-link text-light" href="/festivals">Festival</a>
          {isAdmin && <a className="nav-link text-light" href="/admin">Gestione</a>}
        </div>

        {utente?.autenticato && (
          <div className="d-flex align-items-center gap-2">
            <span className="navbar-text">
              {utente.username}
              {isAdmin && <span className="badge text-bg-warning ms-1">admin</span>}
            </span>
            {/* Logout = POST con il token CSRF, come nel form Thymeleaf (parametro _csrf) */}
            <form method="post" action="/logout">
              <input type="hidden" name="_csrf" value={leggiCookie('XSRF-TOKEN') || ''} />
              <button type="submit" className="btn btn-sm btn-outline-light">Esci</button>
            </form>
          </div>
        )}
        {utente && !utente.autenticato && (
          <div className="d-flex gap-2">
            <a className="btn btn-sm btn-outline-light" href="/login">Accedi</a>
            <a className="btn btn-sm btn-light" href="/register">Registrati</a>
          </div>
        )}
      </div>
    </nav>
  );
}
