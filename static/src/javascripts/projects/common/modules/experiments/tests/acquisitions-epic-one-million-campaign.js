// @flow
import { makeABTest } from 'common/modules/commercial/contributions-utilities';
import { acquisitionsEpicOneMillionCampaignTemplate } from 'common/modules/commercial/templates/acquisitions-epic-one-million-campaign';
import { oneMillionCampaignButtonsTemplate } from 'common/modules/commercial/templates/acquisitions-epic-one-million-campaign-buttons';
import type { EpicTemplate } from 'common/modules/commercial/contributions-utilities';
import {
    getSync as geolocationGetSync,
    getLocalCurrencySymbol,
} from 'lib/geolocation';

const abTestName = 'AcquisitionsEpicOneMillionCampaign';

const australiaCopy = {
    heading: 'We’ve got some news &hellip;',
    paragraphs: [
        '… three years ago, we set out to make The Guardian sustainable by deepening our relationship with our readers. The revenues provided by our print newspaper had diminished. The same technologies that connected us with a global audience also shifted advertising revenues away from news publishers. We decided to seek an approach that would allow us to keep our journalism open and accessible to everyone, regardless of where they live or what they can afford.',
        'Sustained support from our readers enables us to continue pursuing difficult stories in challenging times of political upheaval, when factual reporting has never been more critical. The Guardian is editorially independent – our journalism is free from commercial bias and not influenced by billionaire owners, politicians or shareholders. No one edits our editor. No one steers our opinion. This is important because it enables us to give a voice to the voiceless, challenge the powerful and hold them to account. Readers’ support means we can continue bringing The Guardian’s independent journalism to the world.',
        'If everyone who reads our reporting, who likes it, helps to support it, our future would be much more secure.',
    ],
    highlightedText: `For as little as ${getLocalCurrencySymbol()}1, you can support the Guardian – and it only takes a minute. Thank you.`,
};

const everywhereExceptAustraliaCopy = {
    heading: 'We’ve got some news &hellip;',
    paragraphs: [
        '… three years ago, we set out to make The Guardian sustainable by deepening our relationship with our readers. The revenues provided by our print newspaper had diminished. The same technologies that connected us with a global audience also shifted advertising revenues away from news publishers. We decided to seek an approach that would allow us to keep our journalism open and accessible to everyone, regardless of where they live or what they can afford.',
        'Sustained support from our readers enables us to continue pursuing difficult stories in challenging times of political upheaval, when factual reporting has never been more critical. The Guardian is editorially independent – our journalism is free from commercial bias and not influenced by billionaire owners, politicians or shareholders. No one edits our editor. No one steers our opinion. This is important because it enables us to give a voice to the voiceless, challenge the powerful and hold them to account. Readers’ support means we can continue bringing The Guardian’s independent journalism to the world.',
        'If everyone who reads our reporting, who likes it, helps to support it, our future would be much more secure.',
    ],
    highlightedText: `For as little as ${getLocalCurrencySymbol()}1, you can support the Guardian – and it only takes a minute. Thank you.`,
};

const oneMillionCampaignCopy = (): AcquisitionsEpicTemplateCopy =>
    geolocationGetSync() === 'AU'
        ? australiaCopy
        : everywhereExceptAustraliaCopy;

const oneMillionCampaignTemplate: EpicTemplate = (
    { options = {} },
    copy: AcquisitionsEpicTemplateCopy
) =>
    acquisitionsEpicOneMillionCampaignTemplate({
        copy,
        componentName: options.componentName,
        buttonTemplate: options.buttonTemplate({
            supportUrl: options.supportURL,
        }),
    });

export const acquisitionsEpicOneMillionCampaign: EpicABTest = makeABTest({
    id: abTestName,
    campaignId: abTestName,

    start: '2018-04-17',
    expiry: '2019-06-05',

    author: 'Joseph Smith',
    description: 'Test copy fetched from a Google Doc',
    successMeasure: 'Conversion rate',
    idealOutcome: 'Alternative copy makes more money than the control',

    audienceCriteria: 'All',
    audience: 1,
    audienceOffset: 0,

    variants: [
        {
            id: 'control',
            products: [],
        },
        {
            id: 'just_copy',
            products: [],
            options: {
                copy: oneMillionCampaignCopy,
            },
        },
        {
            id: 'copy_and_design',
            products: [],
            options: {
                template: oneMillionCampaignTemplate,
                copy: oneMillionCampaignCopy,
                buttonTemplate: oneMillionCampaignButtonsTemplate,
            },
        },
    ],
});