import { useState } from 'react';
import { formatData } from '../formato.js';
import ReviewForm from './ReviewForm.jsx';
import Stelle from './Stelle.jsx';

/** Una recensione. Se è dell'utente collegato ("mia") mostra Modifica (form in linea) ed Elimina. */
export default function ReviewItem({ recensione, mia, onModifica, onElimina }) {
  const [inModifica, setInModifica] = useState(false);

  if (inModifica) {
    return (
      <ReviewForm
        titolo="Modifica la tua recensione"
        etichettaInvio="Salva"
        iniziale={{ testo: recensione.testo, voto: recensione.voto }}
        onSubmit={async (valori) => { await onModifica(valori); setInModifica(false); }}
        onAnnulla={() => setInModifica(false)}
      />
    );
  }

  return (
    <div className={`card mb-2 ${mia ? 'border-primary' : ''}`}>
      <div className="card-body">
        <div className="d-flex justify-content-between">
          <div>
            <Stelle voto={recensione.voto} />
            <strong className="ms-2">{recensione.autore}</strong>
            {mia && <span className="badge text-bg-primary ms-2">tua</span>}
          </div>
          <small className="text-muted">{formatData(recensione.data)}</small>
        </div>
        <p className="mb-2 mt-2">{recensione.testo}</p>
        {mia && (
          <div className="d-flex gap-2">
            <button type="button" className="btn btn-sm btn-outline-secondary" onClick={() => setInModifica(true)}>Modifica</button>
            <button type="button" className="btn btn-sm btn-outline-danger"
                    onClick={() => { if (window.confirm('Eliminare la recensione?')) onElimina(); }}>Elimina</button>
          </div>
        )}
      </div>
    </div>
  );
}
