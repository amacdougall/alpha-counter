# Alpha Counter

A single-page life counter webapp for Yomi, written in ClojureScript using the
Om library.

## Compiling ClojureScript

To do a dev build, and recompile on changes:

```sh
lein with-profiles dev do clean, cljsbuild auto alpha-counter
```

To do a production build with vastly smaller filesize:

```sh
lein with-profiles advanced do clean, compile
```

## Compiling CSS

I used a pretty vanilla Foundation install, with Compass. The app-specific
styles are in `src/foundation/scss/app.scss`.

```
bundle install # naturally
bundle exec compass watch src/foundation
```

## Deployment

`deploy.sh` just rsyncs the relevant files to yomicounter.com. If you're a
project collaborator, I can set you up.
