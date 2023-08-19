package com.story.platform.core.domain.workspace

import com.story.platform.core.IntegrationTest
import com.story.platform.core.lib.TestCleaner
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.flow.toList

@IntegrationTest
class WorkspaceRemoverTest(
    private val workspaceRemover: WorkspaceRemover,
    private val workspaceRepository: WorkspaceRepository,
    private val workspaceArchiveRepository: WorkspaceArchiveRepository,
    private val testCleaner: TestCleaner,
) : StringSpec({

    afterEach {
        testCleaner.cleanUp()
    }

    "워크스페이스를 삭제하면 워크스페이스 상태를 삭제된 상태로 변경한다" {
        // given
        val workspace = WorkspaceFixture.create(status = WorkspaceStatus.ENABLED)
        workspaceRepository.save(workspace)

        // when
        workspaceRemover.remove(workspaceId = workspace.workspaceId)

        // then
        val workspaces = workspaceRepository.findAll().toList()
        workspaces shouldHaveSize 1
        workspaces[0].also {
            it.workspaceId shouldBe workspace.workspaceId
            it.plan shouldBe workspace.plan
            it.name shouldBe workspace.name
            it.status shouldBe WorkspaceStatus.DELETED
        }
    }

    "워크스페이스를 삭제하면 아카이빙 워크스페이스에 추가한다" {
        // given
        val workspace = WorkspaceFixture.create(status = WorkspaceStatus.ENABLED)
        workspaceRepository.save(workspace)

        // when
        workspaceRemover.remove(workspaceId = workspace.workspaceId)

        // then
        val workspaceArchives = workspaceArchiveRepository.findAll().toList()
        workspaceArchives shouldHaveSize 1
        workspaceArchives[0].also {
            it.workspaceId shouldBe workspace.workspaceId
            it.plan shouldBe workspace.plan
            it.name shouldBe workspace.name
            it.archiveTime shouldNotBe null
        }
    }

})
