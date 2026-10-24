import { useEffect, useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { Alert, Box, Breadcrumbs, CircularProgress, Link, Typography } from '@mui/material';
import DateFilter from '../components/DateFilter';
import ScreeningsTable from '../components/ScreeningsTable';
import { giorniTra } from '../formato';
import { ApiError } from '../services/api';
import { getFestival, getProgramma } from '../services/festivalService';
import type { Festival, Proiezione } from '../types';

/**
 * Pagina /app/festivals/:id/screenings: programma delle proiezioni di un festival, raggruppato per giorno,
 * con filtro per data. Il giorno scelto sta nell'URL (?date=AAAA-MM-GG), così la pagina filtrata
 * si può ricaricare o condividere.
 */
export default function ScreeningsPage() {
  const { id } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const data = searchParams.get('date') ?? '';

  const [festival, setFestival] = useState<Festival | null>(null);
  const [proiezioni, setProiezioni] = useState<Proiezione[]>([]);
  const [caricamento, setCaricamento] = useState(true);
  const [errore, setErrore] = useState<string | null>(null);

  // Dati del festival (nome e periodo, per costruire l'elenco dei giorni del filtro)
  useEffect(() => {
    getFestival(id!)
      .then(setFestival)
      .catch((e: ApiError) => setErrore(e.message));
  }, [id]);

  // Programma: lo richiedo di nuovo a ogni cambio di filtro, usando il parametro date dell'API
  useEffect(() => {
    async function carica() {
      setCaricamento(true);
      try {
        setProiezioni(await getProgramma(id!, data));
        setErrore(null);
      } catch (e) {
        setErrore((e as ApiError).message);
      } finally {
        setCaricamento(false);
      }
    }
    carica();
  }, [id, data]); // eseguito al mount e ogni volta che cambia il festival o il giorno

  function cambiaData(nuovaData: string) {
    setSearchParams(nuovaData ? { date: nuovaData } : {});
  }

  if (errore) {
    return <Alert severity="error">{errore}</Alert>;
  }

  return (
    <>
      {/* Breadcrumbs verso le pagine Thymeleaf: link normali */}
      <Breadcrumbs sx={{ mb: 2 }}>
        <Link href="/festivals" underline="hover">Festival</Link>
        {festival && <Link href={`/festivals/${festival.id}`} underline="hover">{festival.nome}</Link>}
        <Typography color="text.primary">Programma</Typography>
      </Breadcrumbs>

      <Box sx={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'flex-end', gap: 2, mb: 2 }}>
        <Typography variant="h4" component="h1">Programma delle proiezioni</Typography>
        {festival && (
          <DateFilter giorni={giorniTra(festival.dataInizio, festival.dataFine)} valore={data} onChange={cambiaData} />
        )}
      </Box>

      {caricamento
        ? <Box sx={{ display: 'flex', justifyContent: 'center', py: 5 }}><CircularProgress /></Box>
        : <ScreeningsTable proiezioni={proiezioni} />}
    </>
  );
}
