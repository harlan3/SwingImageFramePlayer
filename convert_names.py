import os
from pathlib import Path

# Set directory path
folder = 'image_frames/'
file_path = folder + 'index.txt'
    
if __name__ == "__main__":
	
	with open(file_path, 'w') as file:
		# Ensure directory exists and list files
		files = sorted(os.listdir(folder))
    
   		# Loop through files, creating a new numbered name
		for i, filename in enumerate(files):
		    # Split extension
		    file_ext = os.path.splitext(filename)[1]
		    
		    # Define new filename with 5-digit padding (e.g., 00001.jpg)
		    if (file_ext == ".jpg"):
		    	new_name = f"{i:05d}{file_ext}"
		    	file.write(Path(new_name).name + "\n")
		    
		    # Rename file
		    if (filename != "index.txt"):
		    	os.rename(
		    	    os.path.join(folder, filename),
		    	    os.path.join(folder, new_name)
		    	)
