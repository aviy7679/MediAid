from transformers import AutoTokenizer, AutoModelForTokenClassification, pipeline

model_name="dmis-lab/biobert-base-cased-v1.2"
# מוריד את הטוקניזר המתאים למודל(הופך מילים לID לפי מה שמוגדר למודל)
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForTokenClassification.from_pretrained(model_name)

biobert_pipeline=pipeline("ner", model=model, tokenizer=tokenizer)

