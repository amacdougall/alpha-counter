# can you believe that this used to be like 25 lines of Ruby?
echo "Compiling JS in advanced mode..."
lein with-profiles advanced do clean, compile
echo "Compilation complete!"
echo "Setting index.html to production version (src/html/index-prod.html)..."
cp src/html/index-prod.html dev-resources/public/index.html
echo "Uploading to remote server as ${USER}..."
if rsync -avz \
    --include "js/main/alpha_counter.js" \
    --exclude "js/main/**" \
    --exclude "**/*.sw[a-p]" \
    dev-resources/public/ \
    $USER@yomicounter.com:/home/amacdougall/webapps/yomicounter ; then
  echo "Upload complete!"
else
  echo "rsync upload failed: are you an authorized SSH user? Contact the project owner for help."
fi

echo "Restoring index.html to dev version (src/html/index-dev.html)..."
cp src/html/index-dev.html dev-resources/public/index.html

echo "Compiling JS in dev mode..."
lein with-profiles dev do clean, compile

echo "Deploy complete!"
