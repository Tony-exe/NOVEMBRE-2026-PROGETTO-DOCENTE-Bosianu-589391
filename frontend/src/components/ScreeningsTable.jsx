import { Link } from 'react-router-dom';
import { formatGiorno, formatOra } from '../formato.js';

const COLORE_STATO = {
  SCHEDULED: 'text-bg-primary',
  COMPLETED: 'text-bg-success',
  CANCELLED: 'text-bg-secondary',
};

/**
 * Proiezioni raggruppate per giorno: una tabella per ogni data.
 * L'API restituisce le proiezioni già ordinate per data e ora, quindi basta raggrupparle.
 */
export default function ScreeningsTable({ proiezioni }) {
  if (proiezioni.length === 0) {
    return <p className="text-muted">Nessuna proiezione in programma.</p>;
  }

  // { "2026-10-26": [p1, p2], "2026-10-27": [p3] } — gli oggetti JS mantengono l'ordine di inserimento
  const perGiorno = proiezioni.reduce((gruppi, p) => {
    (gruppi[p.data] ??= []).push(p);
    return gruppi;
  }, {});

  return Object.entries(perGiorno).map(([giorno, delGiorno]) => (
    <section key={giorno} className="mb-4">
      <h2 className="h5 text-capitalize">{formatGiorno(giorno)}</h2>
      <div className="table-responsive">
        <table className="table bg-white border mb-0">
          <thead className="table-light">
            <tr><th style={{ width: '9rem' }}>Orario</th><th>Film</th><th>Sala</th><th>Stato</th></tr>
          </thead>
          <tbody>
            {delGiorno.map((p) => (
              <tr key={p.id} className={p.stato === 'CANCELLED' ? 'text-muted' : ''}>
                <td>{formatOra(p.ora)}–{formatOra(p.oraFine)}</td>
                <td><Link to={`/movies/${p.film.id}`}>{p.film.nome}</Link></td>
                <td>{p.sala.nome}</td>
                <td><span className={`badge ${COLORE_STATO[p.stato]}`}>{p.stato}</span></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  ));
}
