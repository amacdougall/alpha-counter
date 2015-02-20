if [ "$1" = "prod" ]; then
  HOST='yomicounter.com'
  DIR='/home/amacdougall/webapps/yomicounter'
elif [ "$1" = "staging" ]; then
  HOST="alanmacdougall.com"
  DIR="/home/amacdougall/webapps/alanmacdougall_octopress_blog/alphacounter"
else
  echo "Must provide a target environment argument: staging or prod"
  exit
fi

echo "Compiling JS in advanced mode..."
lein do clean, cljsbuild once prod
echo "Compilation complete!"

echo "host ${HOST}, dir ${DIR}"

echo "Uploading to remote server as ${USER}..."

if rsync -avz \
    --exclude "**/*.sw[a-p]" \
    resources/public/ \
    ${USER}@${HOST}:${DIR} ; then
  echo "Upload complete!"
else
  echo "rsync upload failed: are you an authorized SSH user? Contact the project owner for help."
fi

echo "Compiling JS in dev mode..."
lein do clean, cljsbuild once
echo "Deploy complete!"
