#!/bin/sh

docker run -v "$PWD/astrond.yml:/opt/astron/game/astrond.yml" -v "$PWD/game.dc:/opt/astron/game/game.dc" -p 7198:7198 -p 7199:7199 -it --rm winadam:astron /opt/astron/game/astrond.yml -l debug

