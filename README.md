# cardsagainstlan

## What is this?

It's cards against humanity but as a LAN mobile game for android.

## Why have you done this?

Corona times. Also, the internet at home keeps going down :(

### BUT

LAN games still work when you're not connected to the internet.

This will even work if everybody connects to one guy's hotspot.

I've never been this bored. I was gonna use cordova or something like that but I needed to create DatagramSockets and use multiple threads.


## Where it's at

Peers can start the app, create games and join games and even get as far as reading out a black card / submitting a white card

Nobody can win the game because I only have two devices to test on (solution = make the same app on IOS) (not true, got some more devices to play with, working prototype available).

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
  0xA - AWARD - Tell Host who won the round
  0xB - CROWN - Tell peers who won the round

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

  AWARD
    Peers send AWARD followed by round number, followed by card number, followed by game name to notify peers which card has won the round

    AWARD (int)round (long)card gameName

  CROWN
    Peers send CROWN followed by the card number, followed by the game name, followed by the peer name to notify other peers which card from this round has been awarded by the card czar.

    CROWN (long)card gameName &--& peerName

```

As you can see, very much WIP although the protocol VERBs might not change much anymore.

# Status

Playable Beta, the game works but has some unintended behaviour/crashes

## Issues

We need a log.last that we can retrieve from devices if they crash


Cards whose blank spaces contain 8 or more consecutive underscore characters are treated like cards with two or more blank spaces

--- Any contiguous span of underscores is replaced with exactly six underscores. FIXED

When a player submits two cards for a two card round, the second card is returned to their hand

--- Because the currentHostGame was never told about the second card. HostGame.submitCard can now take a long[] as it's second argument. first card will be submitted last (so the play can be identified by the first card)
--- also the new cards were getting assigned at the start of the next round but they should be assigned immediately after a play is recognized


The game gets less responsive the longer you play it

--- Timeouts for different verbs changed, should make devices more responsive by reducing the amount of messages received per second.
