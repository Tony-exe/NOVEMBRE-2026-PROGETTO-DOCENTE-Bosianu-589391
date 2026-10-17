import { useState } from 'react';
import { ApiError, messaggioErrore } from '../api.js';

/**
 * Form di inserimento/modifica di una recensione (componente "controllato": i valori stanno nello stato).
 * onSubmit è una funzione async fornita dal padre; se il backend risponde con un errore lo mostro qui:
 * 400 → messaggi accanto ai campi, 401/403/409 → messaggio generale.
 */
export default function ReviewForm({ titolo, etichettaInvio, iniziale = { testo: '', voto: 5 }, onSubmit, onAnnulla }) {
  const [testo, setTesto] = useState(iniziale.testo);
  const [voto, setVoto] = useState(iniziale.voto);
  const [invio, setInvio] = useState(false);
  const [erroriCampi, setErroriCampi] = useState({});
  const [errore, setErrore] = useState(null);

  async function gestisciInvio(evento) {
    evento.preventDefault(); // niente invio "classico" del form: la richiesta la fa fetch
    setInvio(true);
    setErrore(null);
    setErroriCampi({});
    try {
      await onSubmit({ testo, voto: Number(voto) });
      if (!onAnnulla) { // form di inserimento: lo svuoto
        setTesto('');
        setVoto(5);
      }
    } catch (e) {
      if (e instanceof ApiError && e.status === 400 && e.problema?.errori) {
        setErroriCampi(e.erroriCampi);
      } else {
        setErrore(messaggioErrore(e));
      }
    } finally {
      setInvio(false);
    }
  }

  return (
    <form onSubmit={gestisciInvio} className="card card-body mb-3" noValidate>
      {titolo && <h3 className="h6">{titolo}</h3>}
      {errore && (
        <div className="alert alert-danger py-2">
          {errore} {errore.startsWith('Devi accedere') && <a href="/login">Accedi</a>}
        </div>
      )}

      <div className="mb-2">
        <label htmlFor="voto" className="form-label">Voto</label>
        <select id="voto" className={`form-select w-auto ${erroriCampi.voto ? 'is-invalid' : ''}`}
                value={voto} onChange={(e) => setVoto(e.target.value)}>
          {[5, 4, 3, 2, 1].map((v) => (
            <option key={v} value={v}>{'★'.repeat(v)} ({v})</option>
          ))}
        </select>
        <div className="invalid-feedback">{erroriCampi.voto}</div>
      </div>

      <div className="mb-2">
        <label htmlFor="testo" className="form-label">Recensione</label>
        <textarea id="testo" rows={3} className={`form-control ${erroriCampi.testo ? 'is-invalid' : ''}`}
                  value={testo} onChange={(e) => setTesto(e.target.value)}
                  placeholder="Almeno 10 caratteri" />
        <div className="invalid-feedback">{erroriCampi.testo}</div>
      </div>

      <div className="d-flex gap-2">
        <button type="submit" className="btn btn-primary" disabled={invio}>
          {invio ? 'Invio…' : etichettaInvio}
        </button>
        {onAnnulla && (
          <button type="button" className="btn btn-outline-secondary" onClick={onAnnulla} disabled={invio}>Annulla</button>
        )}
      </div>
    </form>
  );
}
