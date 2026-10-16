package it.uniroma3.siw.festival.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;

/**
 * Entità associativa Film–Attore: "attore X interpreta il personaggio Y nel film Z".
 * Lo stesso attore può avere più personaggi nello stesso film, ma non lo stesso personaggio due volte.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "film_id", "attore_id", "personaggio" }))
public class Interpretazione {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String personaggio;

    // Lato proprietario di entrambe le relazioni: qui stanno le chiavi esterne film_id e attore_id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "film_id", nullable = false)
    private Film film;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attore_id", nullable = false)
    private Attore attore;

    public Interpretazione() {
    }

    public Interpretazione(Film film, Attore attore, String personaggio) {
        this.film = film;
        this.attore = attore;
        this.personaggio = personaggio;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPersonaggio() {
        return personaggio;
    }

    public void setPersonaggio(String personaggio) {
        this.personaggio = personaggio;
    }

    public Film getFilm() {
        return film;
    }

    public void setFilm(Film film) {
        this.film = film;
    }

    public Attore getAttore() {
        return attore;
    }

    public void setAttore(Attore attore) {
        this.attore = attore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Interpretazione other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Interpretazione.class.hashCode();
    }
}
