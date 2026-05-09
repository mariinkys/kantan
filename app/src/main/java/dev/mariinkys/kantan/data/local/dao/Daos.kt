package dev.mariinkys.kantan.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.mariinkys.kantan.data.local.entity.KanjiEntity
import dev.mariinkys.kantan.data.local.entity.TermEntity
import dev.mariinkys.kantan.data.local.entity.TermFtsEntity

@Dao
interface TermDao {

    @Insert()
    suspend fun insertAll(terms: List<TermEntity>): List<Long>

    @Insert()
    suspend fun insertFts(entries: List<TermFtsEntity>)

    /**
     * Returns ALL rows matching the prefix — including every sense of each word.
     * Grouping into one DictionaryEntry per (expression, reading) is done in the
     * repository, not here, so the detail screen and search see identical data.
     *
     * Ordered so the most relevant word surfaces first within each group:
     *   1. Exact match on reading or expression
     *   2. Shorter reading (more specific)
     *   3. Higher score
     */
    @Query(
        """
        SELECT * FROM terms
        WHERE expression LIKE :prefix || '%'
           OR reading    LIKE :prefix || '%'
        ORDER BY
            CASE WHEN reading = :prefix OR expression = :prefix THEN 0 ELSE 1 END ASC,
            LENGTH(reading) ASC,
            score DESC
        LIMIT :limit
    """
    )
    suspend fun searchByPrefix(prefix: String, limit: Int = 200): List<TermEntity>

    /**
     * FTS search — also returns all rows so the repository can group them.
     * Higher limit because we're merging rows; visible results will be fewer.
     */
    @Query(
        """
        SELECT t.* FROM terms t
        INNER JOIN terms_fts f ON t.id = f.rowid
        WHERE terms_fts MATCH :query
        ORDER BY t.score DESC
        LIMIT :limit
    """
    )
    suspend fun searchByFts(query: String, limit: Int = 200): List<TermEntity>

    @Query("SELECT * FROM terms WHERE sequence IN (:sequences)")
    suspend fun getBySequences(sequences: List<Int>): List<TermEntity>

    @Query(
        """
        SELECT * FROM terms 
        WHERE id >= (ABS(RANDOM()) % (SELECT MAX(id) FROM terms))
          AND (termTags LIKE '%⭐%')
          AND (definitionTags LIKE '%v5r%' OR definitionTags LIKE '%n-pr%')
        LIMIT 1
        """
    )
    suspend fun getRandomCommonTerm(): TermEntity?

    @Query("DELETE FROM terms")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM terms")
    suspend fun count(): Int
}

@Dao
interface KanjiDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(kanji: List<KanjiEntity>)

    @Query("SELECT * FROM kanji WHERE character = :char LIMIT 1")
    suspend fun getByCharacter(char: String): KanjiEntity?

    @Query("SELECT * FROM kanji WHERE character IN (:chars)")
    suspend fun getByCharacters(chars: List<String>): List<KanjiEntity>

    @Query("DELETE FROM kanji")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM kanji")
    suspend fun count(): Int
}