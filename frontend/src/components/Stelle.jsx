/** Voto da 1 a 5 mostrato come stelle: ★★★★☆ */
export default function Stelle({ voto }) {
  return (
    <span className="text-warning" title={`${voto} su 5`} aria-label={`${voto} su 5`}>
      {'★'.repeat(voto)}{'☆'.repeat(5 - voto)}
    </span>
  );
}
