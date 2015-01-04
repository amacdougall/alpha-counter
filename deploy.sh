# can you believe that this used to be like 25 lines of Ruby?
rsync -avz --include "js/main/alpha_counter.js" --exclude "js/main/**" --exclude "**/*.sw[a-p]" dev-resources/public/ $USER@yomicounter.com:/home/amacdougall/webapps/yomicounter
