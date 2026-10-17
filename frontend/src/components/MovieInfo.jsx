import { Link } from 'react-router-dom';
import { formatData, formatOra } from '../formato.js';

/** Informazioni del film: dati principali, regista, cast, festival e proiezioni. Solo visualizzazione. */
export default function MovieInfo({ film }) {
  const { regista } = film;

  return (
    <div className="bg-white border rounded-3 p-4 mb-4">
      <h1 className="h3">{film.titolo}</h1>
      <p className="text-muted">
        {film.anno} · {film.genere} · {film.durata} min · {film.paeseProduzione}
      </p>
      <p>
        <strong>Regia:</strong> {regista.nome} {regista.cognome}
        {regista.nazionalita && <span className="text-muted"> ({regista.nazionalita})</span>}
      </p>

      <div className="row g-4">
        <div className="col-md-6">
          <h2 className="h5">Cast</h2>
          {film.cast.length === 0 ? (
            <p className="text-muted">Cast non disponibile.</p>
          ) : (
            <ul className="list-unstyled">
              {film.cast.map((i) => (
                <li key={`${i.attoreId}-${i.personaggio}`}>
                  {i.nome} {i.cognome} <span className="text-muted">— {i.personaggio}</span>
                </li>
              ))}
            </ul>
          )}

          <h2 className="h5 mt-3">Festival</h2>
          {film.festivals.length === 0 ? (
            <p className="text-muted">Non partecipa a nessun festival.</p>
          ) : (
            <ul className="list-unstyled">
              {/* Pagina Thymeleaf: link normale */}
              {film.festivals.map((f) => (
                <li key={f.id}><a href={`/festivals/${f.id}`}>{f.nome}</a> <span className="text-muted">{f.citta}</span></li>
              ))}
            </ul>
          )}
        </div>

        <div className="col-md-6">
          <h2 className="h5">Proiezioni</h2>
          {film.proiezioni.length === 0 ? (
            <p className="text-muted">Nessuna proiezione in programma.</p>
          ) : (
            <ul className="list-group">
              {film.proiezioni.map((p) => (
                <li key={p.id}
                    className={`list-group-item d-flex justify-content-between ${p.stato === 'CANCELLED' ? 'text-muted text-decoration-line-through' : ''}`}>
                  <span>
                    {formatData(p.data)}, {formatOra(p.ora)} · {p.sala.nome}
                    <br />
                    {/* Pagina React: Link del router, senza ricaricare la pagina */}
                    <Link to={`/festivals/${p.festival.id}/screenings`} className="small">{p.festival.nome}</Link>
                  </span>
                  {p.stato !== 'SCHEDULED' && <span className="badge text-bg-secondary align-self-start">{p.stato}</span>}
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}
