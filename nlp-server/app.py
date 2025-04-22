from flask import Flask, request, jsonify
# from text_symptom_extraction.symptom_pipeline import extract_and_normalize_symptoms
from image_symptom_extraction.predictor import predict
app = Flask(__name__)


@app.route('/')
def hello_world():  
    return "hi"

# @app.route("/extract_symptoms", methods=["POST"])
# def extract_symptoms():
#     user_input = request.json.get("text", "")
#     results = extract_and_normalize_symptoms(user_input)
#     return jsonify(results)

@app.route('/predict-from-path', methods=['POST'])
def predict_from_path():
    data = request.get_json()
    if not data or "path" not in data:
        return jsonify({"error": "Missing 'path' in JSON"}), 400

    image_path = data["path"]
    try:
        predictions = predict(image_path)
        return jsonify(predictions)
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run()
