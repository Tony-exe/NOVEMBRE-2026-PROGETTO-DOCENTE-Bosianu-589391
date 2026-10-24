// Formattazione di date e orari che arrivano dalle API in formato ISO ("2026-10-26", "18:00:00").

/**
 * "2026-10-26" → Date locale. Non uso new Date("2026-10-26"): la interpreterebbe come mezzanotte UTC
 * e in alcuni fusi orari mostrerebbe il giorno precedente.
 */
export function daIso(dataIso: string): Date {
  const [anno, mese, giorno] = dataIso.split('-').map(Number);
  return new Date(anno, mese - 1, giorno);
}

/** "2026-10-26" → "lun 26 ott 2026" */
export function formatData(dataIso: string): string {
  return daIso(dataIso).toLocaleDateString('it-IT', { weekday: 'short', day: '2-digit', month: 'short', year: 'numeric' });
}

/** "2026-10-26" → "lunedì 26 ottobre" (intestazioni del programma) */
export function formatGiorno(dataIso: string): string {
  return daIso(dataIso).toLocaleDateString('it-IT', { weekday: 'long', day: 'numeric', month: 'long' });
}

/** "18:00:00" → "18:00" */
export function formatOra(oraIso: string): string {
  return oraIso.substring(0, 5);
}

/** Tutti i giorni da inizio a fine compresi, in formato ISO (per il filtro per data). */
export function giorniTra(inizioIso: string, fineIso: string): string[] {
  const giorni: string[] = [];
  const giorno = daIso(inizioIso);
  const fine = daIso(fineIso);
  while (giorno <= fine) {
    const mese = String(giorno.getMonth() + 1).padStart(2, '0');
    const g = String(giorno.getDate()).padStart(2, '0');
    giorni.push(`${giorno.getFullYear()}-${mese}-${g}`);
    giorno.setDate(giorno.getDate() + 1);
  }
  return giorni;
}
