package it.uniroma3.siw.festival.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
public class Film {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String titolo;

    @NotNull
    @Min(1888)
    @Max(2100)
    private Integer anno;

    /** Durata in minuti: serve a calcolare l'ora di fine delle proiezioni. */
    @NotNull
    @Min(1)
    @Max(600)
    private Integer durata;

    @NotBlank
    private String genere;

    @NotBlank
    private String paeseProduzione;

    /*
     * Per JPA il default delle @ManyToOne è EAGER: senza LAZY esplicito ogni film caricato
     * si porterebbe dietro il regista anche quando non serve. Con LAZY Hibernate mette un proxy
     * e carica il regista solo al primo accesso (fonte del problema N+1 se si accede in un ciclo):
     * quando serve lo carichiamo con JOIN FETCH o @EntityGraph (vedi FilmRepository).
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "regista_id", nullable = false)
    private Regista regista;

    // Lato inverso della molti-a-molti: il proprietario è Festival.films
    @ManyToMany(mappedBy = "films")
    private Set<Festival> festivals = new HashSet<>();

    /*
     * Le interpretazioni (il cast) sono "parti" del film: cascade ALL le salva/cancella insieme
     * al film, e orphanRemoval cancella dal DB un'interpretazione tolta dalla lista.
     */
    @OneToMany(mappedBy = "film", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Interpretazione> interpretazioni = new ArrayList<>();

    // Proiezioni e recensioni hanno un ciclo di vita proprio: nessun cascade
    @OneToMany(mappedBy = "film")
    private List<Proiezione> proiezioni = new ArrayList<>();

    @OneToMany(mappedBy = "film")
    private List<Recensione> recensioni = new ArrayList<>();

    public Film() {
    }

    public Film(String titolo, Integer anno, Integer durata, String genere, String paeseProduzione,
            Regista regista) {
        this.titolo = titolo;
        this.anno = anno;
        this.durata = durata;
        this.genere = genere;
        this.paeseProduzione = paeseProduzione;
        this.regista = regista;
    }

    public void addInterpretazione(Attore attore, String personaggio) {
        Interpretazione interpretazione = new Interpretazione(this, attore, personaggio);
        interpretazioni.add(interpretazione);
        attore.getInterpretazioni().add(interpretazione);
    }

    public void removeInterpretazione(Interpretazione interpretazione) {
        interpretazioni.remove(interpretazione);
        interpretazione.getAttore().getInterpretazioni().remove(interpretazione);
    }

    public void addProiezione(Proiezione proiezione) {
        proiezioni.add(proiezione);
        proiezione.setFilm(this);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitolo() {
        return titolo;
    }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }

    public Integer getAnno() {
        return anno;
    }

    public void setAnno(Integer anno) {
        this.anno = anno;
    }

    public Integer getDurata() {
        return durata;
    }

    public void setDurata(Integer durata) {
        this.durata = durata;
    }

    public String getGenere() {
        return genere;
    }

    public void setGenere(String genere) {
        this.genere = genere;
    }

    public String getPaeseProduzione() {
        return paeseProduzione;
    }

    public void setPaeseProduzione(String paeseProduzione) {
        this.paeseProduzione = paeseProduzione;
    }

    public Regista getRegista() {
        return regista;
    }

    public void setRegista(Regista regista) {
        this.regista = regista;
    }

    public Set<Festival> getFestivals() {
        return festivals;
    }

    public List<Interpretazione> getInterpretazioni() {
        return interpretazioni;
    }

    public List<Proiezione> getProiezioni() {
        return proiezioni;
    }

    public List<Recensione> getRecensioni() {
        return recensioni;
    }

    // equals/hashCode sull'id: stessa logica spiegata in Festival
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Film other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Film.class.hashCode();
    }
}
