import { useEffect, useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { api, messaggioErrore } from '../api.js';
import { giorniTra } from '../formato.js';
import DateFilter from '../components/DateFilter.jsx';
import ScreeningsTable from '../components/ScreeningsTable.jsx';

/**
 * Pagina /app/festivals/:id/screenings: programma delle proiezioni di un festival, raggruppato per giorno,
 * con filtro per data. Il giorno scelto sta nell'URL (?date=AAAA-MM-GG), così la pagina filtrata
 * si può ricaricare o condividere.
 */
export default function ScreeningsPage() {
  const { id } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const data = searchParams.get('date') || '';

  const [festival, setFestival] = useState(null);
  const [proiezioni, setProiezioni] = useState([]);
  const [caricamento, setCaricamento] = useState(true);
  const [errore, setErrore] = useState(null);

  // Dati del festival (nome e periodo, per costruire l'elenco dei giorni del filtro)
  useEffect(() => {
    api.get(`/api/festivals/${id}`)
      .then(setFestival)
      .catch((e) => setErrore(messaggioErrore(e)));
  }, [id]);

  // Programma: lo richiedo di nuovo a ogni cambio di filtro, usando il parametro date dell'API
  useEffect(() => {
    setCaricamento(true);
    const url = data ? `/api/festivals/${id}/screenings?date=${data}` : `/api/festivals/${id}/screenings`;
    api.get(url)
      .then((risposta) => { setProiezioni(risposta); setErrore(null); })
      .catch((e) => setErrore(messaggioErrore(e)))
      .finally(() => setCaricamento(false));
  }, [id, data]);

  function cambiaData(nuovaData) {
    setSearchParams(nuovaData ? { date: nuovaData } : {});
  }

  if (errore) {
    return <div className="alert alert-danger">{errore}</div>;
  }

  return (
    <>
      <nav aria-label="breadcrumb">
        <ol className="breadcrumb">
          <li className="breadcrumb-item"><a href="/festivals">Festival</a></li>
          {festival && <li className="breadcrumb-item"><a href={`/festivals/${festival.id}`}>{festival.nome}</a></li>}
          <li className="breadcrumb-item active">Programma</li>
        </ol>
      </nav>

      <div className="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-3">
        <h1 className="h3 m-0">Programma delle proiezioni</h1>
        {festival && (
          <DateFilter giorni={giorniTra(festival.dataInizio, festival.dataFine)} valore={data} onChange={cambiaData} />
        )}
      </div>

      {caricamento
        ? <p className="text-muted"><span className="spinner-border spinner-border-sm me-2" />Caricamento…</p>
        : <ScreeningsTable proiezioni={proiezioni} />}
    </>
  );
}
