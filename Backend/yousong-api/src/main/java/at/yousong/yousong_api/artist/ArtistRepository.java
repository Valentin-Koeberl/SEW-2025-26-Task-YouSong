package at.yousong.yousong_api.artist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArtistRepository extends JpaRepository<Artist, Long> {

    // Suche (Teilstring)
    List<Artist> findByNameContainingIgnoreCase(String name);

    // Exakte Suche (für DataLoader praktisch)
    Optional<Artist> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
