import spacy
import json


class ScispaCySymptomExtractor:
    """
    A class that extracts medical entities from text using ScispaCy.
    This is a simpler alternative to MedCAT that still offers medical NER.
    """

    def __init__(self, model_name="en_core_sci_md"):
        """
        Initialize with a ScispaCy model.

        Args:
            model_name: The name of the ScispaCy model to use
                        (options: en_core_sci_sm, en_core_sci_md, en_core_sci_lg)
        """
        print(f"Loading ScispaCy model: {model_name}...")
        try:
            # Load the ScispaCy model
            self.nlp = spacy.load(model_name)
            print("Model loaded successfully")

            # Print available entity types for reference
            print(f"Available entity types: {self.nlp.get_pipe('ner').labels}")

        except OSError:
            print(f"Model '{model_name}' not found.")
            print("\nTo install ScispaCy models, run these commands:")
            print("pip install scispacy")
            if model_name == "en_core_sci_md":
                print(
                    "pip install https://s3-us-west-2.amazonaws.com/ai2-s2-scispacy/releases/v0.5.0/en_core_sci_md-0.5.0.tar.gz")
            elif model_name == "en_core_sci_sm":
                print(
                    "pip install https://s3-us-west-2.amazonaws.com/ai2-s2-scispacy/releases/v0.5.0/en_core_sci_sm-0.5.0.tar.gz")
            elif model_name == "en_core_sci_lg":
                print(
                    "pip install https://s3-us-west-2.amazonaws.com/ai2-s2-scispacy/releases/v0.5.0/en_core_sci_lg-0.5.0.tar.gz")
            raise
        except Exception as e:
            print(f"Error loading model: {e}")
            raise

    def extract_medical_entities(self, text):
        """
        Extract medical entities from text.

        Args:
            text (str): Free text input describing medical conditions

        Returns:
            list: List of extracted medical entities
        """
        if not text or not isinstance(text, str):
            return []

        try:
            # Process the text with SpaCy
            doc = self.nlp(text)

            # Extract entities
            entities = []
            for ent in doc.ents:
                entities.append({
                    'text': ent.text,
                    'label': ent.label_,
                    'start': ent.start_char,
                    'end': ent.end_char
                })

            return entities

        except Exception as e:
            print(f"Error extracting entities: {e}")
            return []

    def extract_symptoms(self, text):
        """
        Extract potential symptoms from text.
        Filters for likely symptom-related entities.

        Args:
            text (str): Free text input describing medical conditions

        Returns:
            list: List of extracted symptoms
        """
        entities = self.extract_medical_entities(text)

        # Filter for entity types likely to be symptoms
        # ScispaCy uses different labels than MedCAT:
        # - DISEASE: diseases and symptoms
        # - CHEMICAL: medications
        # - PROCEDURE: medical procedures
        # etc.
        symptom_labels = ['DISEASE', 'PROBLEM', 'FINDING']

        symptoms = [
            {
                'text': entity['text'],
                'type': entity['label'],
                'name': entity['text']  # ScispaCy doesn't normalize names
            }
            for entity in entities
            if entity['label'] in symptom_labels
        ]

        return symptoms


# Example usage
def demonstrate_scispacy_extraction():
    """Demonstrate symptom extraction using ScispaCy"""

    try:
        # Initialize the extractor
        extractor = ScispaCySymptomExtractor()

        # Example text
        sample_text = "The patient reports severe headache, fever, and fatigue. They also mentioned having a sore throat and cough for the past 3 days."

        # Extract all medical entities
        print("\n=== All Medical Entities ===")
        entities = extractor.extract_medical_entities(sample_text)
        print(json.dumps(entities, indent=2))

        # Extract symptoms specifically
        print("\n=== Symptoms Only ===")
        symptoms = extractor.extract_symptoms(sample_text)
        print(json.dumps(symptoms, indent=2))

        # Print symptom summary
        print("\nSymptom Summary:")
        if symptoms:
            for s in symptoms:
                print(f"- {s['text']} ({s['type']})")
        else:
            print("No symptoms found")

    except Exception as e:
        print(f"Demonstration failed: {e}")
        print("Please ensure you have ScispaCy and the required models installed")


if __name__ == "__main__":
    demonstrate_scispacy_extraction()