from huggingface_hub import snapshot_download

snapshot_download(repo_id="kaushalya/medclip", local_dir="image_symptom_extraction/medclip")
