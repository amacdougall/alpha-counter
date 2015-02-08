# can you believe that this used to be like 25 lines of Ruby?
echo "Compiling JS in advanced mode..."
lein with-profiles -dev,+uberjar do cljsbuild clean, cljsbuild once
echo "Compilation complete!"
echo "Uploading to remote server as ${USER}..."

# rsync resources/public to the server; within the js folder, include only
# app.js, discarding the umpteen source-map-related output files.
if rsync -avz \
	--dry-run \
    --include "js/app.js" \
    --exclude "js/*" \
    --exclude "**/*.sw[a-p]" \
    resources/public/ \
    $USER@yomicounter.com:/home/amacdougall/webapps/yomicounter ; then
  echo "Upload complete!"
else
  echo "rsync upload failed: are you an authorized SSH user? Contact the project owner for help."
fi

echo "Compiling JS in dev mode..."
lein do cljsbuild clean, cljsbuild once

echo "Deploy complete!"
