clean:
	rm -rf target

run:
	clj -M:dev

repl:
	clj -M:dev:nrepl

repltest:
	clj -M:test:nrepl

tests:
	clj -M:test && tput bel

uberjarlight:
	npm run tailwind && clj -T:build all && tput bel
