export default function DiagnosisScreen() {
    return (
        <div>
            <h1>Medical Diagnosis</h1>
            <label>
                Enter Textual Description:
                <input type="text" name="description" />
            </label>
            <button>Browse My Files</button>
            <input type="file"></input>
            <button>Open Camera</button>
            <div>
                <button>Via Image</button>
                <button>Via Description</button>
            </div>
        </div>
    );
}