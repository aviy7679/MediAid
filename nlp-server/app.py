from flask import Flask, request, jsonify
from text_symptom_extraction.symptom_pipeline import extract_and_normalize_symptoms

app = Flask(__name__)


@app.route('/')
def hello_world():  
    return "hi"

@app.route("/extract_symptoms", methods=["POST"])
def extract_symptoms():
    user_input = request.json.get("text", "")
    results = extract_and_normalize_symptoms(user_input)
    return jsonify(results)

if __name__ == '__main__':
    app.run()
