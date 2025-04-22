import spacy
"""
from scispacy.umls_linking import  UmlsEntityLinker

#משתנים פרטיים
_nlp_linker = None
_linker = None

def get_nlp_linker():
    global _nlp_linker, _linker
    if _nlp_linker is None:
        _nlp_linker = spacy.load("en_core_sci_lg")
        if "scispacy" not in _nlp_linker.pipe_names:
            _linker=UmlsEntityLinker(resolve_abbreviations=True, name="umls")
            _nlp_linker.add_pipe("scispacy_linker", config={"resolve_abbreviations": True, "linker_name": "umls"})
        else:
            _linker=_nlp_linker.get_pipe("scispacy_linker")
    return _nlp_linker, _linker


def normalize_symptoms(symptoms_text):
    nlp, linker = get_nlp_linker()
    doc = nlp(symptoms_text)
    for ent in doc.ents:
        for umls_ent in ent._.umls_ents:
            concept=linker.umls.cui_to_entuty[umls_ent[0]]
            return {
                "original": symptoms_text,
                "canonical_name": concept.canonical_name,
                "cui": concept.cui
            }
    return {
        "original": symptoms_text,
        "canonical_name": None,
        "cui": None,
    }
    """
