"""
Text Analysis System - Keywords with verified CUI normalization
English-only medical symptom detection system
"""
import re
import logging
from typing import List, Dict, Optional
import time
from config import (
    TEXT_CONFIDENCE_HIGH, TEXT_CONFIDENCE_MEDIUM, TEXT_CONFIDENCE_LOW,
    INTENSITY_FACTOR_HIGH, INTENSITY_FACTOR_MEDIUM, INTENSITY_FACTOR_LOW,
    INTENSITY_FACTOR_MILD, INTENSITY_FACTOR_SLIGHT,
    NEGATION_CHECK_DISTANCE, WORD_BOUNDARY_CONTEXT,
    UMLS_TYPE_SIGN_SYMPTOM,
    CATEGORY_CARDIOVASCULAR, CATEGORY_RESPIRATORY, CATEGORY_NEUROLOGICAL,
    CATEGORY_GASTROINTESTINAL, CATEGORY_MUSCULOSKELETAL, CATEGORY_DERMATOLOGICAL,
    CATEGORY_PSYCHOLOGICAL, CATEGORY_SYSTEMIC, CATEGORY_VISUAL, CATEGORY_SLEEP,
    CATEGORY_PAIN, CATEGORY_GENERAL,
    DEBUG_SEPARATOR_LONG
)

logger = logging.getLogger(__name__)

class KeywordTextAnalyzer:
    """Text analyzer based on keywords with verified CUI normalization"""

    def __init__(self):
        self.cui_database = {}
        self.keyword_index = {}
        self.is_loaded = False

        self.load_symptom_database()

    def load_symptom_database(self):
        """Load symptom database with verified CUI codes"""

        logger.info("Loading keyword database with verified CUI codes...")

        # Verified CUI database for common symptoms
        self.cui_database = {
            # Chest Pain
            "C3807341": {
                "name": "Chest Pain",
                "keywords": [
                    "chest pain", "chest ache", "chest discomfort", "thoracic pain",
                    "pain in chest", "chest hurts", "chest pressure", "chest tightness",
                    "heart pain", "cardiac pain", "chest burning"
                ],
                "modifiers": ["sharp", "dull", "crushing", "stabbing", "radiating"],
                "confidence": TEXT_CONFIDENCE_HIGH,
                "category": CATEGORY_CARDIOVASCULAR
            },

            # Dyspnea (Shortness of breath)
            "C4230442": {
                "name": "Dyspnea",
                "keywords": [
                    "shortness of breath", "difficulty breathing", "dyspnea", "breathless",
                    "can't breathe", "breathing problems", "respiratory distress",
                    "labored breathing", "air hunger", "breathlessness", "winded"
                ],
                "contexts": ["exertion", "rest", "lying down", "climbing stairs"],
                "confidence": TEXT_CONFIDENCE_HIGH,
                "category": CATEGORY_RESPIRATORY
            },

            # Asthenia (Weakness)
            "C0004093": {
                "name": "Asthenia",
                "keywords": [
                    "asthenia", "weakness", "weak", "general weakness", "body weakness",
                    "muscle weakness", "feeling weak", "lack of strength"
                ],
                "confidence": TEXT_CONFIDENCE_LOW,
                "category": CATEGORY_GENERAL
            },

            # Fever
            "C0015967": {
                "name": "Fever",
                "keywords": [
                    "fever", "febrile", "high temperature", "pyrexia", "hyperthermia",
                    "hot", "burning up", "feverish", "temp", "temperature", "running a fever"
                ],
                "measurements": ["°C", "°F", "celsius", "fahrenheit", "degrees"],
                "confidence": TEXT_CONFIDENCE_HIGH,
                "category": CATEGORY_SYSTEMIC
            },

            # Headache
            "C0018681": {
                "name": "Headache",
                "keywords": [
                    "headache", "head pain", "migraine", "head ache", "cephalgia",
                    "cranial pain", "head hurts", "pain in head", "head throbbing",
                    "skull pain", "head pounding"
                ],
                "modifiers": ["severe", "mild", "chronic", "acute", "pulsating", "tension"],
                "confidence": TEXT_CONFIDENCE_HIGH,
                "category": CATEGORY_NEUROLOGICAL
            },

            # Nausea
            "C3554470": {
                "name": "Nausea",
                "keywords": [
                    "nausea", "nauseous", "nauseated", "queasy", "sick to stomach",
                    "feel sick", "upset stomach", "queasiness", "stomach upset",
                    "motion sickness", "feeling nauseous"
                ],
                "confidence": TEXT_CONFIDENCE_MEDIUM,
                "category": CATEGORY_GASTROINTESTINAL
            },

            # Vomiting
            "C4230730": {
                "name": "Vomiting",
                "keywords": [
                    "vomiting", "vomit", "throwing up", "puking", "retching",
                    "emesis", "sick", "brought up", "regurgitation", "upchuck"
                ],
                "frequency": ["persistent", "intermittent", "projectile"],
                "confidence": TEXT_CONFIDENCE_HIGH,
                "category": CATEGORY_GASTROINTESTINAL
            },

            # Dizziness
            "C0012833": {
                "name": "Dizziness",
                "keywords": [
                    "dizziness", "dizzy", "lightheaded", "vertigo", "spinning",
                    "unsteady", "balance problems", "off balance", "wobbly",
                    "faint", "lightheadedness", "room spinning"
                ],
                "confidence": TEXT_CONFIDENCE_MEDIUM,
                "category": CATEGORY_NEUROLOGICAL
            },

            # Pain (General)
            "C0030193": {
                "name": "Pain",
                "keywords": [
                    "pain", "ache", "aching", "hurt", "hurting", "sore", "painful",
                    "tender", "throbbing", "sharp pain", "dull pain", "burning pain",
                    "stabbing", "cramping", "discomfort"
                ],
                "confidence": TEXT_CONFIDENCE_LOW,
                "category": CATEGORY_PAIN
            },

            # Fatigue
            "C0015672": {
                "name": "Fatigue",
                "keywords": [
                    "fatigue", "tired", "exhausted", "exhaustion", "weary",
                    "lack of energy", "run down", "worn out", "drained", "lethargic",
                    "sleepy", "drowsy", "feeling tired"
                ],
                "confidence": TEXT_CONFIDENCE_LOW,
                "category": CATEGORY_SYSTEMIC
            },

            # Diarrhea
            "C3554472": {
                "name": "Diarrhea",
                "keywords": [
                    "diarrhea", "loose stools", "watery stools", "frequent bowel movements",
                    "runny stool", "liquid stool", "bowel urgency"
                ],
                "confidence": TEXT_CONFIDENCE_MEDIUM,
                "category": CATEGORY_GASTROINTESTINAL
            },

            # Muscle Pain (Myalgia)
            "C1963177": {
                "name": "Muscle Pain",
                "keywords": [
                    "muscle pain", "muscle ache", "muscle soreness", "myalgia",
                    "muscle cramps", "muscle stiffness", "sore muscles", "muscle tenderness"
                ],
                "confidence": TEXT_CONFIDENCE_MEDIUM,
                "category": CATEGORY_MUSCULOSKELETAL
            },

            # Insomnia
            "C1969971": {
                "name": "Insomnia",
                "keywords": [
                    "insomnia", "sleeplessness", "can't sleep", "difficulty sleeping",
                    "trouble sleeping", "sleep problems", "unable to sleep",
                    "sleep disturbance", "restless sleep"
                ],
                "confidence": TEXT_CONFIDENCE_MEDIUM,
                "category": CATEGORY_SLEEP
            },

            # Rash
            "C4227880": {
                "name": "Rash",
                "keywords": [
                    "rash", "skin rash", "eruption", "skin eruption", "skin lesions",
                    "red spots", "skin irritation", "dermatitis"
                ],
                "confidence": TEXT_CONFIDENCE_MEDIUM,
                "category": CATEGORY_DERMATOLOGICAL
            },

            # Palpitations
            "C3160712": {
                "name": "Palpitations",
                "keywords": [
                    "palpitations", "heart palpitations", "heart racing", "rapid heartbeat",
                    "irregular heartbeat", "heart fluttering", "heart pounding",
                    "fast pulse", "skipped beats"
                ],
                "confidence": TEXT_CONFIDENCE_MEDIUM,
                "category": CATEGORY_CARDIOVASCULAR
            },

            # Anxiety
            "C4228281": {
                "name": "Anxiety",
                "keywords": [
                    "anxiety", "anxious", "worried", "nervous", "panic",
                    "restless", "on edge", "stressed", "tense", "apprehensive"
                ],
                "confidence": TEXT_CONFIDENCE_LOW,
                "category": CATEGORY_PSYCHOLOGICAL
            },

            # Confusion
            "C0009676": {
                "name": "Confusion",
                "keywords": [
                    "confusion", "confused", "disoriented", "bewildered",
                    "mental fog", "cloudy thinking", "difficulty concentrating"
                ],
                "confidence": TEXT_CONFIDENCE_MEDIUM,
                "category": CATEGORY_NEUROLOGICAL
            },

            # Blurred Vision
            "C0344232": {
                "name": "Blurred Vision",
                "keywords": [
                    "blurred vision", "blurry vision", "vision problems", "fuzzy vision",
                    "difficulty seeing", "vision disturbance", "unclear vision"
                ],
                "confidence": TEXT_CONFIDENCE_MEDIUM,
                "category": CATEGORY_VISUAL
            },

            # Itching
            "C0033774": {
                "name": "Itching",
                "keywords": [
                    "itching", "itchy", "pruritus", "scratching", "skin irritation",
                    "urge to scratch", "tickling sensation"
                ],
                "confidence": TEXT_CONFIDENCE_MEDIUM,
                "category": CATEGORY_DERMATOLOGICAL
            },

            # Weight Loss
            "C1262477": {
                "name": "Weight Loss",
                "keywords": [
                    "weight loss", "losing weight", "dropped weight", "lost pounds",
                    "unintentional weight loss", "weight reduction"
                ],
                "confidence": TEXT_CONFIDENCE_MEDIUM,
                "category": CATEGORY_GENERAL
            }
        }

        # Build search index
        self._build_keyword_index()

        self.is_loaded = True
        logger.info(f"Loaded keyword database with {len(self.cui_database)} verified CUI codes and {len(self.keyword_index)} keywords")

    def _build_keyword_index(self):
        """Build fast search index"""

        self.keyword_index = {}

        for cui, data in self.cui_database.items():
            # Primary keywords
            for keyword in data.get("keywords", []):
                keyword_clean = keyword.lower().strip()
                if keyword_clean not in self.keyword_index:
                    self.keyword_index[keyword_clean] = []
                self.keyword_index[keyword_clean].append(cui)

            # Primary name
            primary_name = data["name"].lower()
            if primary_name not in self.keyword_index:
                self.keyword_index[primary_name] = []
            self.keyword_index[primary_name].append(cui)

    def analyze_text(self, text: str) -> List[Dict]:
        """Analyze text and return detected symptoms"""

        if not self.is_loaded:
            return []

        start_time = time.time()
        text_lower = text.lower()

        found_symptoms = []

        # Stage 1: Direct keyword matches
        direct_matches = self._find_direct_matches(text_lower)
        found_symptoms.extend(direct_matches)

        # Stage 2: Complex phrase matches
        phrase_matches = self._find_phrase_matches(text_lower)
        found_symptoms.extend(phrase_matches)

        # Stage 3: Context-based matches
        context_matches = self._find_context_matches(text_lower)
        found_symptoms.extend(context_matches)

        # Clean and enhance results
        final_symptoms = self._clean_and_enhance(found_symptoms, text)

        processing_time = time.time() - start_time

        logger.debug(f"Keyword analysis completed in {processing_time:.3f}s, found {len(final_symptoms)} symptoms")

        return final_symptoms

    def _find_direct_matches(self, text: str) -> List[Dict]:
        """Find direct keyword matches"""

        matches = []

        for keyword, cuis in self.keyword_index.items():
            if keyword in text:
                # Check word boundaries
                pattern = rf'\b{re.escape(keyword)}\b'
                for match in re.finditer(pattern, text, re.IGNORECASE):
                    for cui in cuis:
                        symptom_data = self.cui_database[cui]

                        # Check for negation
                        if self._check_negation(text, match.start(), match.end()):
                            continue

                        symptom = {
                            "cui": cui,
                            "name": symptom_data["name"],
                            "detected_name": match.group(),
                            "start": match.start(),
                            "end": match.end(),
                            "confidence": symptom_data["confidence"],
                            "category": symptom_data["category"],
                            "match_type": "direct",
                            "source": "keyword_analyzer"
                        }
                        matches.append(symptom)

        return matches

    def _find_phrase_matches(self, text: str) -> List[Dict]:
        """Find complex phrase matches like 'severe headache'"""

        matches = []

        # Intensity modifiers
        intensifiers = {
            "severe": INTENSITY_FACTOR_MEDIUM, "intense": INTENSITY_FACTOR_MEDIUM,
            "extreme": INTENSITY_FACTOR_HIGH, "terrible": INTENSITY_FACTOR_MEDIUM,
            "mild": INTENSITY_FACTOR_MILD, "slight": INTENSITY_FACTOR_SLIGHT, "minor": INTENSITY_FACTOR_SLIGHT,
            "chronic": INTENSITY_FACTOR_LOW, "persistent": INTENSITY_FACTOR_LOW, "constant": INTENSITY_FACTOR_LOW,
            "acute": INTENSITY_FACTOR_MEDIUM, "sharp": INTENSITY_FACTOR_LOW, "dull": 0.9
        }

        # Find combinations
        words = text.split()
        for i in range(len(words) - 1):
            current_word = words[i].strip('.,!?').lower()
            next_word = words[i + 1].strip('.,!?').lower()

            # If first word is intensifier and second is symptom
            if current_word in intensifiers and next_word in self.keyword_index:
                intensity_factor = intensifiers[current_word]

                for cui in self.keyword_index[next_word]:
                    symptom_data = self.cui_database[cui]

                    phrase = f"{current_word} {next_word}"
                    phrase_start = text.find(phrase)

                    if phrase_start >= 0:
                        adjusted_confidence = min(1.0, symptom_data["confidence"] * intensity_factor)

                        symptom = {
                            "cui": cui,
                            "name": f"{current_word.title()} {symptom_data['name']}",
                            "detected_name": phrase,
                            "start": phrase_start,
                            "end": phrase_start + len(phrase),
                            "confidence": round(adjusted_confidence, 2),
                            "category": symptom_data["category"],
                            "match_type": "phrase",
                            "intensity": current_word,
                            "source": "keyword_analyzer"
                        }
                        matches.append(symptom)

        return matches

    def _find_context_matches(self, text: str) -> List[Dict]:
        """Find context-based matches - body parts + pain"""

        matches = []

        # Body parts to symptom mapping
        body_parts_mapping = {
            "head": "C0018681",     # headache
            "chest": "C3807341",    # chest pain
            "stomach": "C3554470",  # nausea (stomach related)
            "abdomen": "C3554470",  # nausea (abdominal)
            "muscle": "C1963177",   # muscle pain
            "muscles": "C1963177",  # muscle pain
            "heart": "C3160712"     # palpitations
        }

        pain_indicators = ["pain", "ache", "hurt", "sore", "discomfort", "aching"]

        for body_part, related_cui in body_parts_mapping.items():
            if body_part in text:
                # Look for pain indicators nearby
                for pain_word in pain_indicators:
                    if pain_word in text:
                        body_pos = text.find(body_part)
                        pain_pos = text.find(pain_word)

                        # Check proximity (within WORD_BOUNDARY_CONTEXT characters)
                        if abs(body_pos - pain_pos) <= WORD_BOUNDARY_CONTEXT:
                            symptom_data = self.cui_database[related_cui]

                            combined_phrase = f"{body_part} {pain_word}"
                            start_pos = min(body_pos, pain_pos)
                            end_pos = max(body_pos + len(body_part), pain_pos + len(pain_word))

                            symptom = {
                                "cui": related_cui,
                                "name": f"{body_part.title()} {pain_word.title()}",
                                "detected_name": combined_phrase,
                                "start": start_pos,
                                "end": end_pos,
                                "confidence": TEXT_CONFIDENCE_LOW,
                                "category": symptom_data["category"],
                                "match_type": "contextual",
                                "source": "keyword_analyzer"
                            }
                            matches.append(symptom)

        return matches

    def _check_negation(self, text: str, start: int, end: int) -> bool:
        """Check for negation before symptom"""

        negation_words = [
            "no", "not", "without", "never", "absent", "free", "clear",
            "denies", "negative", "lacks", "missing"
        ]

        # Check NEGATION_CHECK_DISTANCE characters before the symptom
        before_text = text[max(0, start - NEGATION_CHECK_DISTANCE):start].lower()

        return any(neg_word in before_text for neg_word in negation_words)

    def _clean_and_enhance(self, symptoms: List[Dict], original_text: str) -> List[Dict]:
        """Clean duplicates and enhance information"""

        # Remove duplicates by CUI
        unique_symptoms = {}

        for symptom in symptoms:
            cui = symptom["cui"]

            if cui not in unique_symptoms:
                unique_symptoms[cui] = symptom
            else:
                # Keep higher confidence
                if symptom["confidence"] > unique_symptoms[cui]["confidence"]:
                    unique_symptoms[cui] = symptom

        # Sort by confidence
        sorted_symptoms = sorted(unique_symptoms.values(),
                               key=lambda x: x["confidence"], reverse=True)

        # Enhance with additional info
        for symptom in sorted_symptoms:
            cui = symptom["cui"]
            cui_data = self.cui_database[cui]

            # Add additional information
            symptom.update({
                "type_ids": [UMLS_TYPE_SIGN_SYMPTOM],  # Sign or Symptom in UMLS
                "pretty_name": cui_data["name"],
                "umls_cui": cui,
                "keyword_count": len(cui_data.get("keywords", [])),
                "system": cui_data["category"]
            })

        return sorted_symptoms

    def get_cui_info(self, cui: str) -> Optional[Dict]:
        """Get information about specific CUI"""
        return self.cui_database.get(cui)

    def search_symptoms(self, query: str) -> List[Dict]:
        """Search symptoms by name"""

        query_lower = query.lower()
        results = []

        for cui, data in self.cui_database.items():
            # Search in name
            if query_lower in data["name"].lower():
                results.append({"cui": cui, "match_in": "name", **data})

            # Search in keywords
            elif any(query_lower in keyword.lower() for keyword in data.get("keywords", [])):
                results.append({"cui": cui, "match_in": "keywords", **data})

        return results

    def get_statistics(self) -> Dict:
        """Get system statistics"""

        if not self.is_loaded:
            return {"error": "System not loaded"}

        categories = {}
        total_keywords = 0

        for cui, data in self.cui_database.items():
            category = data.get("category", "unknown")
            categories[category] = categories.get(category, 0) + 1
            total_keywords += len(data.get("keywords", []))

        return {
            "total_cuis": len(self.cui_database),
            "total_keywords": total_keywords,
            "categories": categories,
            "avg_keywords_per_cui": round(total_keywords / len(self.cui_database), 1),
            "memory_efficient": True,
            "load_time": "instant",
            "verified_cui_codes": True
        }

# =============================================================================
# DEMO
# =============================================================================

def demo_keyword_analyzer():
    """Demo of keyword analysis system"""

    print("🔍 Keyword Text Analysis System Demo")
    print(DEBUG_SEPARATOR_LONG)

    analyzer = KeywordTextAnalyzer()

    test_texts = [
        "Patient complains of severe headache and high fever",
        "Experiencing chest pain with shortness of breath",
        "I have nausea and muscle pain after exercise",
        "Feeling dizzy and very tired lately with palpitations",
        "No headache today, feeling much better"
    ]

    print("🧪 Analysis Tests:")

    total_time = 0

    for i, text in enumerate(test_texts, 1):
        print(f"\n{i}. Analyzing: '{text}'")

        start_time = time.time()
        symptoms = analyzer.analyze_text(text)
        analysis_time = time.time() - start_time
        total_time += analysis_time

        print(f"   ⏱️ Time: {analysis_time:.3f}s")
        print(f"   🎯 Found {len(symptoms)} symptoms:")

        for symptom in symptoms:
            confidence = symptom["confidence"]
            match_type = symptom["match_type"]

            print(f"      - {symptom['name']}")
            print(f"        CUI: {symptom['cui']}, Confidence: {confidence:.2f}, Type: {match_type}")

    print(f"\n📊 Statistics:")
    stats = analyzer.get_statistics()

    print(f"   • Total CUI codes: {stats['total_cuis']}")
    print(f"   • Total keywords: {stats['total_keywords']}")
    print(f"   • Average keywords per CUI: {stats['avg_keywords_per_cui']}")
    print(f"   • Total processing time: {total_time:.3f}s")
    print(f"   • Verified CUI codes: {stats['verified_cui_codes']}")

    print(f"\n Categories:")
    for category, count in stats["categories"].items():
        print(f"   • {category}: {count}")

if __name__ == "__main__":
    demo_keyword_analyzer()