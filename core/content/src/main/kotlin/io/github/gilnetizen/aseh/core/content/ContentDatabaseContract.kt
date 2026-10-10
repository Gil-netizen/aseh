package io.github.gilnetizen.aseh.core.content

/** Stable cross-runtime contract for immutable content-pack SQLite databases. */
object ContentDatabaseContract {
    const val FILE_NAME = "content.sqlite"
    const val APPLICATION_ID = 0x41534548
    const val SEARCH_TABLE = "content_fts"
    const val SEARCH_ENGINE = "fts4"

    val requiredMetadataKeys: Set<String> = setOf(
        "content_schema",
        "pack_id",
        "pack_type",
        "pack_version",
        "review_state",
        "search_engine",
        "source_tree_sha256",
    )

    val requiredTableNames: Set<String> = setOf(
        "pack_metadata",
        "editions",
        "content_documents",
        "content_prerequisites",
        "claims",
        "citations",
        SEARCH_TABLE,
        "content_fts_content",
        "content_fts_segments",
        "content_fts_segdir",
        "content_fts_docsize",
        "content_fts_stat",
    )
}
