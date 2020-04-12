# cardsagainstlan

## What is this?

It's cards against humanity but as a LAN mobile game.

## Why have you done this?

Corona times. Also, the internet at home keeps going down :(

### BUT

LAN games still work when you're not connected to the internet.

This will even work if everybody connects to one guy's hotspot.

I've never been this bored. I was gonna use cordova or something like that but I needed to create DatagramSockets and use multiple threads.


## Where it's at

Peers can start the app, create games and join games and even get as far as reading out a black card / submitting a white card

Nobody can win the game because I only have two devices to test on (solution = make the same app on IOS).

Most of the protocol is defined:

```

VERB byte[2]
  0x1 - POKE - Tell other peers you are online
  0x2 - INVITE - Tell other peers you are a host
  0x3 - JOIN - Tell host peer you want to join
  0x4 - CLAIM - Tell other peers you have claimed a name
  0x5 - CONTEST - Tell other peer a name is taken
  0x6 - ROUND - Tell other peer what round it is
  0x7 - DEAL - Give other peer cards
  0x8 - PLAY - Submit card to card-czar (and game host)
  0x9 - DECK - Ask for cards

  POKE:
    Peers send POKE followed by their name to notify other peers of their presence on the network.

    Peers keep a list of all known peers. If a peer does not send POKE for 6500 ms it is removed from this list but can come back at any time

  INVITE:
    Peers send INVITE followed by the gameName then their name to notify other peers that they are hosting a game
    INVITE gameName &--& userName

  JOIN:
    Peers send JOIN followed by their name then the gameName to notify other peers that they are joining a game
    JOIN userName &--& gameName

  ROUND
    Peers send ROUND followed by the round number (int) then the black card (long) and the game name to inform peers about the game state

    ROUND (int)roundNumber (long)card gameName &--& czarName

  DEAL
    If a peer is hosting a game and receives DECK, they should send a DEAL, followed by round number (int), followed by 10 cards (expressed as one long each), followed by the gameName, followed by their own name.

    DEAL (int) round (long[10]) cards gameName &--& peerName

  PLAY
    Peers send PLAY followed by the gameName, followed by their own name, followed by a card (long) to notify other peers that they have played a card

  DECK
    Peers send DECK followed by game name followed by peer name to ask host peers what cards they have

    PLAY gameName &--& peerName

```

The rest haven't been defined yet but will probably include:

```
VERB byte[2]
  0xa - AWARD - say who the round winner is
  0xb - SCORE - say what someone's score is

```

As you can see, very much WIP.
