# alpha-counter

An Om-based life counter for the Yomi fighting card game.

Based on the totally kickass Figwheel; see instructions at
https://github.com/bhauman/lein-figwheel.

## Running Locally

```sh
bundle install
bundle exec compass compile src/foundation
lein figwheel
```

Now open localhost:3449 and you're golden.

## Deploying

1. Modify deploy.sh to point at a server you have SSH access to! Right now it
   points to alanmacdougall.com and yomicounter.com.
2. `./deploy.sh staging` or `./deploy.sh prod` will build the app with advanced
   compilation, rsync to the appropriate server, and rebuild in dev mode for
   continued local development.


