'''''
import spacy

_nlp_ner=None

def get_ner_model():
    global _nlp_ner
    if _nlp_ner is None:
        _nlp_ner=spacy.load("en_ner_bionlp13cg_md")
    return _nlp_ner

def detect_symptoms(text):
    nlp=get_ner_model()
    doc = _nlp_ner(text)
    symptoms = [ent.text for ent in doc.ents if ent.label_ == 'SIGN_OR_SYMPTOM']
    return symptoms
'''''

import networkx as nx
import matplotlib.pyplot as plt

# הגדרת הצמתים עם מזהי CUI ושמות
nodes = {
    "C0000737": "גיל: 65 שנים",
    "C0086582": "מין: זכר",
    "C0020538": "יתר לחץ דם",
    "C0008031": "כאב חזה",
    "C0013798": "ECG חריג",
    "C0027051": "אוטם שריר הלב",
    "C0004057": "אספירין",
    "C0010068": "צנתור כלילי",
    "C0018787": "תפקוד לב תקין"
}

# הגדרת הקשרים בין הצמתים
edges = [
    ("C0020538", "C0008031", "מעלה סיכון ל-"),
    ("C0008031", "C0013798", "מעיד על"),
    ("C0013798", "C0027051", "מוביל ל-"),
    ("C0004057", "C0027051", "מטפל ב-"),
    ("C0010068", "C0027051", "מטפל ב-"),
    ("C0027051", "C0018787", "הגעה ל-")
]

# יצירת הגרף
G = nx.DiGraph()

# הוספת הצמתים
for cui, label in nodes.items():
    G.add_node(cui, label=label)

# הוספת הקשתות
for source, target, relation in edges:
    G.add_edge(source, target, label=relation)

# ציור הגרף
plt.figure(figsize=(14, 10))
pos = nx.spring_layout(G, seed=42)
node_labels = nx.get_node_attributes(G, 'label')
edge_labels = nx.get_edge_attributes(G, 'label')

nx.draw(G, pos, with_labels=False, node_size=3000, node_color="skyblue", font_size=10, font_weight='bold', arrows=True)
nx.draw_networkx_labels(G, pos, labels=node_labels, font_size=10)
nx.draw_networkx_edge_labels(G, pos, edge_labels=edge_labels, font_size=9)

plt.title("תרשים מסלול רפואי עם מזהי CUI", fontsize=14)
plt.axis('off')
plt.tight_layout()
plt.show()