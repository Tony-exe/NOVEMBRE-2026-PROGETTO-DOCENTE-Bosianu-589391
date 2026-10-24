// Interfacce TypeScript che replicano ESATTAMENTE i DTO Java del backend (package dto.api).
// Se un DTO cambia, si aggiorna qui e il compilatore segnala tutti i punti del codice da sistemare.
// Date e orari arrivano come stringhe ISO: LocalDate → "2026-10-26", LocalTime → "18:00:00".

export interface Festival {
  id: number;
  nome: string;
  anno: number;
  citta: string;
  dataInizio: string;
  dataFine: string;
  descrizione: string | null;
}

export interface Regista {
  id: number;
  nome: string;
  cognome: string;
  dataNascita: string | null;
  nazionalita: string | null;
}

/** Un attore del cast con il personaggio interpretato (FilmDettaglioDto.Interprete). */
export interface Interprete {
  attoreId: number;
  nome: string;
  cognome: string;
  personaggio: string;
}

export type StatoProiezione = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED'; // union type = enum Java

/** Riferimento compatto a un'altra risorsa (ProiezioneDto.Riferimento). */
export interface Riferimento {
  id: number;
  nome: string;
}

export interface Proiezione {
  id: number;
  data: string;
  ora: string;
  oraFine: string;
  stato: StatoProiezione;
  film: Riferimento;
  sala: Riferimento;
  festival: Riferimento;
}

export interface FilmDettaglio {
  id: number;
  titolo: string;
  anno: number;
  durata: number;
  genere: string;
  paeseProduzione: string;
  regista: Regista;
  cast: Interprete[];
  festivals: Festival[];
  proiezioni: Proiezione[];
  mediaVoti: number | null;
  numeroRecensioni: number;
}

export interface Recensione {
  id: number;
  filmId: number;
  testo: string;
  voto: number;
  data: string;
  autore: string;
}

export interface RecensioniFilm {
  filmId: number;
  mediaVoti: number | null;
  numero: number;
  recensioni: Recensione[];
}

/** Corpo di POST/PUT di una recensione (RecensioneForm): film e autore NON ci sono, li decide il backend. */
export interface NuovaRecensione {
  testo: string;
  voto: number;
}

/** GET /api/me */
export interface UtenteCorrente {
  autenticato: boolean;
  username: string | null;
  ruolo: 'USER' | 'ADMIN' | null;
}
