package com.thoughtworks.pli.intellij.remotepair.protocol

import com.thoughtworks.pli.intellij.remotepair.MySpecification

class SyncFilesSpec extends MySpecification {

  "Some events" should {
    def forwardToMaster(event: PairEvent) = new ProtocolMocking {
      client(context1, context2, context3).createOrJoinProject("test")
      client(context2).beMaster()
      client(context1).send(event)
      there was one(context2).writeAndFlush(event.toMessage)
      there was no(context1).writeAndFlush(event.toMessage)
      there was no(context3).writeAndFlush(event.toMessage)
    }

    "be forwarded to master only" in new ProtocolMocking {
      forwardToMaster(syncFilesRequest)
    }
  }

  "If server receives some event with 'toClientId', it should only forward it to correct client" should {
    def check(creator: String => PairEvent) = new ProtocolMocking {
      client(context1, context2, context3).createOrJoinProject("test")
      val event = creator(clientId(context2))
      client(context1).send(event)
      there was one(context2).writeAndFlush(event.toMessage)
      there was no(context3).writeAndFlush(event.toMessage)
    }
    "for MasterPairableFiles" in check(MasterPairableFiles("from-id", _, Nil, 2))
    "for SyncFileEvent" in check(SyncFileEvent("from-id", _, "/aaa", Content("abc", "UTF-8")))
    "for PairableFiles" in check(PairableFiles("from-id", _, Seq(FileSummary("/aaa", "md5"))))
    "for GetPairableFilesFromPair" in check(GetPairableFilesFromPair("from-id", _))
  }

  "If server receives SyncFilesForAll, it" should {
    "broadcast it to all non-master clients" in new ProtocolMocking {
      client(context1, context2, context3).createOrJoinProject("test")
      // context2 is not master, context1 is
      client(context2).send(SyncFilesForAll)

      there was no(context1).writeAndFlush(SyncFilesForAll.toMessage)
      there was one(context2).writeAndFlush(SyncFilesForAll.toMessage)
      there was one(context3).writeAndFlush(SyncFilesForAll.toMessage)
    }
  }

}
