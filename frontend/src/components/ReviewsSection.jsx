import { useCallback, useEffect, useState } from 'react';
import { api, messaggioErrore } from '../api.js';
import ReviewForm from './ReviewForm.jsx';
import ReviewItem from './ReviewItem.jsx';

/**
 * Recensioni di un film: elenco, media dei voti, inserimento (utente loggato che non ha ancora recensito),
 * modifica ed eliminazione delle proprie. Dopo ogni scrittura ricarica l'elenco dal server.
 * I pulsanti di modifica compaiono solo sulle proprie recensioni, ma è il backend a garantire la regola (403).
 */
export default function ReviewsSection({ filmId, utente }) {
  const [dati, setDati] = useState(null);       // { mediaVoti, numero, recensioni }
  const [caricamento, setCaricamento] = useState(true);
  const [errore, setErrore] = useState(null);
  const [messaggio, setMessaggio] = useState(null);

  // useCallback: la stessa funzione tra un render e l'altro, così può stare tra le dipendenze di useEffect
  const carica = useCallback(() => {
    return api.get(`/api/movies/${filmId}/reviews`)
      .then((risposta) => { setDati(risposta); setErrore(null); })
      .catch((e) => setErrore(messaggioErrore(e)))
      .finally(() => setCaricamento(false));
  }, [filmId]);

  useEffect(() => { carica(); }, [carica]);

  // Le funzioni di scrittura rilanciano l'errore: il form lo usa per mostrare i messaggi accanto ai campi
  async function crea(valori) {
    await api.post(`/api/movies/${filmId}/reviews`, valori);
    setMessaggio('Recensione pubblicata');
    await carica();
  }

  async function modifica(id, valori) {
    await api.put(`/api/reviews/${id}`, valori);
    setMessaggio('Recensione aggiornata');
    await carica();
  }

  async function elimina(id) {
    try {
      await api.delete(`/api/reviews/${id}`);
      setMessaggio('Recensione eliminata');
      await carica();
    } catch (e) {
      setErrore(messaggioErrore(e));
    }
  }

  if (caricamento) {
    return <p className="text-muted"><span className="spinner-border spinner-border-sm me-2" />Caricamento recensioni…</p>;
  }

  const recensioni = dati?.recensioni ?? [];
  const miaRecensione = utente?.autenticato && recensioni.find((r) => r.autore === utente.username);

  return (
    <section>
      <div className="d-flex align-items-baseline gap-3 mb-3">
        <h2 className="h4 m-0">Recensioni</h2>
        {dati?.mediaVoti != null && (
          <span className="text-muted">
            media <strong>{dati.mediaVoti.toFixed(1)}</strong> / 5 su {dati.numero} {dati.numero === 1 ? 'recensione' : 'recensioni'}
          </span>
        )}
      </div>

      {messaggio && (
        <div className="alert alert-success alert-dismissible">
          {messaggio}
          <button type="button" className="btn-close" aria-label="Chiudi" onClick={() => setMessaggio(null)} />
        </div>
      )}
      {errore && <div className="alert alert-danger">{errore}</div>}

      {/* Cosa mostrare sopra l'elenco dipende da chi sta guardando */}
      {utente && !utente.autenticato && (
        <p><a href="/login">Accedi</a> per scrivere una recensione.</p>
      )}
      {utente?.autenticato && !miaRecensione && (
        <ReviewForm titolo="Scrivi la tua recensione" etichettaInvio="Pubblica" onSubmit={crea} />
      )}

      {recensioni.length === 0 ? (
        <p className="text-muted">Ancora nessuna recensione.</p>
      ) : (
        recensioni.map((r) => (
          <ReviewItem
            key={r.id}
            recensione={r}
            mia={utente?.autenticato && r.autore === utente.username}
            onModifica={(valori) => modifica(r.id, valori)}
            onElimina={() => elimina(r.id)}
          />
        ))
      )}
    </section>
  );
}
